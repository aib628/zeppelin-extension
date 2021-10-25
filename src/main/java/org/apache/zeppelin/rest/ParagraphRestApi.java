package org.apache.zeppelin.rest;


import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import org.apache.zeppelin.annotation.ZeppelinApi;
import org.apache.zeppelin.beans.AnalysisResult;
import org.apache.zeppelin.interpreter.Interpreter;
import org.apache.zeppelin.interpreter.InterpreterNotFoundException;
import org.apache.zeppelin.interpreter.util.SqlSplitter;
import org.apache.zeppelin.notebook.AuthorizationService;
import org.apache.zeppelin.notebook.Note;
import org.apache.zeppelin.notebook.Notebook;
import org.apache.zeppelin.notebook.Paragraph;
import org.apache.zeppelin.notebook.ParagraphTextParser;
import org.apache.zeppelin.rest.exception.ForbiddenException;
import org.apache.zeppelin.rest.exception.NoteNotFoundException;
import org.apache.zeppelin.rest.exception.ParagraphNotFoundException;
import org.apache.zeppelin.server.JsonResponse;
import org.apache.zeppelin.service.AuthenticationService;

@Singleton
@Path("/notebook")
@Produces("application/json")
public class ParagraphRestApi extends AbstractRestApi {

    private final Notebook notebook;
    private final AuthorizationService authorizationService;
    private final AuthenticationService authenticationService;

    @Inject
    public ParagraphRestApi(
            Notebook notebook,
            AuthorizationService authorizationService,
            AuthenticationService authenticationService
    ) {
        super(authenticationService);
        this.notebook = notebook;
        this.authorizationService = authorizationService;
        this.authenticationService = authenticationService;
    }

    @GET
    @ZeppelinApi
    @Path("{noteId}/paragraph/{paragraphId}/analysis")
    public Response getParagraph(@PathParam("noteId") String noteId,
                                 @PathParam("paragraphId") String paragraphId) throws IOException, InterpreterNotFoundException {

        Note note = notebook.getNote(noteId);
        checkIfNoteIsNotNull(note, noteId);
        checkIfUserCanRead(noteId);

        Paragraph paragraph = note.getParagraph(paragraphId);
        checkIfParagraphIsNotNull(paragraph, paragraphId);

        AnalysisResult analysisResult = new AnalysisResult(ParagraphTextParser.parse(paragraph.getText()));
        analysisResult.getScripts().addAll(new SqlSplitter().splitSql(analysisResult.getScriptText()));

        for (List<Interpreter> session : paragraph.getBindedInterpreter().getInterpreterGroup().values()) {
            for (Interpreter interpreter : session) {
                analysisResult.getConfigurations().putAll(interpreter.getProperties());
            }
        }

        return new JsonResponse<>(Response.Status.OK, analysisResult).build();
    }

    private void checkIfNoteIsNotNull(Note note, String noteId) {
        if (note == null) {
            throw new NoteNotFoundException(noteId);
        }
    }

    private void checkIfParagraphIsNotNull(Paragraph paragraph, String paragraphId) {
        if (paragraph == null) {
            throw new ParagraphNotFoundException(paragraphId);
        }
    }

    /**
     * Check if the current user can access (at least he have to be reader) the given note.
     */
    private void checkIfUserCanRead(String noteId) {
        Set<String> userAndRoles = Sets.newHashSet();
        userAndRoles.add(authenticationService.getPrincipal());
        userAndRoles.addAll(authenticationService.getAssociatedRoles());
        if (!authorizationService.hasReadPermission(userAndRoles, noteId)) {
            throw new ForbiddenException("Insufficient privileges you cannot get this paragraph");
        }
    }

}
