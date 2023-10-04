package bitTorrents;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

public class MyCustomFormater extends SimpleFormatter {
    @Override
    public String format(LogRecord record) {
        StringBuffer sb = new StringBuffer();
        sb.append(record.getMessage());
        sb.append("\n");
        return sb.toString();
    }
}
