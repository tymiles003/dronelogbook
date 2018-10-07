package ugcs.upload.logbook;

import lombok.SneakyThrows;
import ugcs.csv.telemetry.TelemetryCsvWriter;
import ugcs.processing.telemetry.FlightTelemetry;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import static ugcs.csv.telemetry.TelemetryCsvWriter.CSV_FILE_CHARSET;
import static ugcs.upload.logbook.UploadResponse.fromList;

public class LogBookUploader {
    private static final List<String> FILED_CODES = Arrays.asList(
            "Time",
            "latitude",
            "longitude",
            "altitude_agl",
            "ground_speed",
            "main_voltage",
            "main_current"
    );

    private final String serverUrl;
    private final String login;
    private final String rawPasswordOrMd5Hash;

    public LogBookUploader(String serverUrl, String login, String rawPasswordOrMd5Hash) {
        this.serverUrl = serverUrl;
        this.login = login;
        this.rawPasswordOrMd5Hash = rawPasswordOrMd5Hash;
    }

    @SneakyThrows
    public FlightUploadResponse uploadFlight(FlightTelemetry flight) {
        final File csvFile = File.createTempFile(flight.getVehicle().getName(), "");
        try (final OutputStream out = new FileOutputStream(csvFile)) {
            final TelemetryCsvWriter telemetryWriter = new TelemetryCsvWriter(FILED_CODES, out);
            telemetryWriter.printHeader();
            flight.getTelemetry().forEach(timeAndTelemetry ->
                    telemetryWriter.printTelemetryRecord(timeAndTelemetry.getLeft(), timeAndTelemetry.getRight())
            );
        }

        return new FlightUploadResponse(flight, csvFile, fromList(uploadFile(csvFile)));
    }

    private List<String> uploadFile(File file) {
        MultipartUtility multipart = new MultipartUtility(serverUrl, CSV_FILE_CHARSET.displayName());

        multipart.withCredentials(login, rawPasswordOrMd5Hash);
        multipart.addFilePart("data", file);

        return multipart.finish();
    }
}