package gg.bonka.mirage.filesystem;

/**
 * The AsyncWorldDirectoryCallback interface defines a callback method to be called
 * after an asynchronous operation on the worlds directory is completed.
 * <br><br>
 * <b>This callback may not be executed in the main thread!</b>
 */
public interface AsyncWorldDirectoryCallback {

    /**
     * The {@code callback} method is a callback function that is called after an asynchronous operation is completed.
     * It is used to inform the caller about the success or failure of the operation and provide an optional message.
     * <br><br>
     * <b>This callback may not be executed in the main thread!</b>
     *
     * @param success a boolean indicating the success status of the operation
     * @param message a string providing more information about the operation (optional)
     */
    void callback(boolean success, String message);
}
