package projctx.famazing.utility;

import android.annotation.TargetApi;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;

/**
 * Class that improves the classic EdiText, adding the possibility to get the components of the date written in the EditText.
 */
public class DateEditText extends EditText {

    private Integer year, month, day;

    public DateEditText(Context context) {
        super(context);
    }

    public DateEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DateEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(21)
    public DateEditText(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        super.setText(text, type);
        if (text != null && text.length() > 0) {
            try {
                year = Integer.valueOf(text.subSequence(0, 4).toString());
            } catch (NumberFormatException ignored) {}
            try {
                month = Integer.valueOf(text.subSequence(5, 7).toString());
            } catch (NumberFormatException ignored) {}
            try {
                day = Integer.valueOf(text.subSequence(8, text.length()).toString());
            } catch (NumberFormatException ignored) {}
        }
    }

    /**
     * Return the year represented in the edit text.
     * @return the year in the edit text or null if the date is not correctly represented.
     */
    public Integer getYear() {
        return year;
    }

    /**
     * Return the month represented in the edit text.
     * @return the month in the edit text or null if the date is not correctly represented.
     */
    public Integer getMonth() {
        return month;
    }

    /**
     * Return the day represented in the edit text.
     * @return the day in the edit text or null if the date is not correctly represented.
     */
    public Integer getDay() {
        return day;
    }

    /**
     * States if the date in the edit text represent the correct format on which the system relies.
     * @return true if the date is in format "YYYY-MM-DD", false otherwise.
     */
    public boolean isDateComplete() {
        return year != null && month != null && day != null;
    }
}
