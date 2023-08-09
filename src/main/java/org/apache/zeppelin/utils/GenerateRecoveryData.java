package org.apache.zeppelin.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class GenerateRecoveryData {

    private static final String RECOVERY_FILE_PATH = "/opt/zeppelin/recovery";
    private static final String RECOVERY_FILE_NAME = "flink.recovery";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Usage: {interpreterGroupId} {HostAndPort}"); //flink-2HTYRQ479 10.140.151.46:31549
            System.exit(1);
        }

        List<String> recoveryData = new ArrayList<>(readRecoveryData());
        recoveryData.add(args[0] + "\t" + args[1]);

        System.out.print(String.join(System.lineSeparator(), new HashSet<>(recoveryData)));
    }

    private static List<String> readRecoveryData() throws Exception {
        File recoveryFile = new File(RECOVERY_FILE_PATH, RECOVERY_FILE_NAME);
        try (InputStream inputs = new FileInputStream(recoveryFile)) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[2048];
            int len;

            while ((len = inputs.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }

            bos.close();
            return readRecoveryData(new String(bos.toByteArray()));
        }
    }

    private static List<String> readRecoveryData(String recoveryData) {
        return Arrays.stream(recoveryData.split(System.lineSeparator())).filter(it -> !it.isEmpty()).collect(Collectors.toList());
    }

}
