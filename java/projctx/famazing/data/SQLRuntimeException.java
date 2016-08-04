package projctx.famazing.data;

/**
 * Wrapper exception for SQLException that is launched everytime an SQLException is generated, making the SQLException a RuntimeException in fact.
 */
public class SQLRuntimeException extends RuntimeException {

    public SQLRuntimeException() {
        super();
    }

    public SQLRuntimeException(String message) {
        super(message);
    }
}
