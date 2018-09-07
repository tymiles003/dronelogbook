package ugcs.upload.logbook;

import ugcs.processing.telemetry.FlightTelemetry;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FlightUploadResponse {
    private final FlightTelemetry flightTelemetry;
    private final File flightFile;
    private final UploadResponse uploadResponse;

    FlightUploadResponse(FlightTelemetry flightTelemetry, File flightFile, UploadResponse uploadResponse) {
        this.flightTelemetry = flightTelemetry;
        this.flightFile = flightFile;
        this.uploadResponse = uploadResponse;
    }

    public FlightTelemetry getFlightTelemetry() {
        return flightTelemetry;
    }

    public UploadResponse getUploadResponse() {
        return uploadResponse;
    }

    public void storeFlightTelemetry(Path targetPath) {
        try {
            Files.move(flightFile.toPath(), targetPath);
        } catch (IOException toRethrow) {
            throw new RuntimeException(toRethrow);
        }
    }
}