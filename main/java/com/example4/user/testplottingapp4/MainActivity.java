package com.example4.user.testplottingapp4;

import android.content.Context;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import com.example4.user.testplottingapp4.Calculator.Calculator;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.Series;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    public static final String FUNCTION_X_SYMBOL   = "x";
    public static       int    POINTS_RADIUS       = 10;
    public static final int    DOTS_COUNT_MIN      = 10;
    public static final int    DOTS_COUNT_MAX      = 1000;
    public static final double DEFINITION_AREA_MIN = -100000;
    public static final double DEFINITION_AREA_MAX = 100000;

    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate (savedInstanceState);
        setContentView (R.layout.activity_main);

        // Вешаем слушатель на кнопку построения графика.
        final Button button = findViewById (R.id.buttonDrawPlot);
        button.setOnClickListener (new View.OnClickListener () {
            public void onClick (View _v) {

                // Скроем клавиатуру.
                InputMethodManager inputManager = (InputMethodManager) getSystemService (Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow ((null == getCurrentFocus ()) ? null : getCurrentFocus ().getWindowToken (),
                        InputMethodManager.HIDE_NOT_ALWAYS);

                try {
                    // Если поля корректны, рисуем график.
                    if (isEditTextsAreValid ()) {
                        drawPlot ();
                    }
                } catch (Exception e) {
                    // Какое-то непредвиденное исключение.
                    showErrorAlert (String.format (getResources ().getString (R.string.unknown_exception_error_message), e.getClass ().toString ().toString (), e.getMessage ()));
                }
            }
        });
    }

    /**
     * Нарисовать график.
     */
    protected void drawPlot () {
        // Найдем нужные элементы.
        GraphView graph                      = (GraphView) findViewById (R.id.graphMain);
        EditText  editTextFunction           = (EditText) findViewById (R.id.editTextFunction);
        EditText  editTextDotsCount          = (EditText) findViewById (R.id.editTextDotsCount);
        EditText  editTextDefinitionAreaFrom = (EditText) findViewById (R.id.editTextDefinitionAreaFrom);
        EditText  editTextDefinitionAreaTo   = (EditText) findViewById (R.id.editTextDefinitionAreaTo);

        // Очистим график от прошлых данных.
        graph.removeAllSeries ();


        //-------------------------------
        // Получим числа из полей ввода и переведем в нужные типы данных.
        // На всей области определения не должно быть некорректных операций. Например, деления на ноль и т.п.
        // Иначе будет выброшено исключение и график не будет построен.
        String functionText = editTextFunction.getText ().toString ();
        double xMin         = Double.valueOf (editTextDefinitionAreaFrom.getText ().toString ());
        double xMax         = Double.valueOf (editTextDefinitionAreaTo.getText ().toString ());
        int    countDots    = Integer.valueOf (editTextDotsCount.getText ().toString ());

        //-------------------------------
        // Получим шаг цикла.
        double xDelta = (xMax - xMin) / countDots;

        // Set manual bounds
        graph.getViewport ().setXAxisBoundsManual (true);
        graph.getViewport ().setMinX (xMin);
        graph.getViewport ().setMaxX (xMax);

        // Заполняем серию точек.
        LineGraphSeries<DataPoint> seriesDots = new LineGraphSeries<> ();
        try {
            for (double i = xMin; i < xMax; i += xDelta) {
                DataPoint point = new DataPoint (i, Calculator.calculateNormal (functionText.replace (FUNCTION_X_SYMBOL, String.valueOf (i))));
                seriesDots.appendData (point, true, countDots);
            }
        } catch (Calculator.CalculatorNaNException e) {
            // Исключение в случае недопустимых математических операций.
            showErrorAlert (String.format (getResources ().getString (R.string.nan_error_message), e.getMessage ()));
            return;
        } catch (Calculator.CalculatorErrorException e) {
            // Исключение в случае других непредвиденных ошибок класса калькулятора.
            showErrorAlert (e.getMessage ());
            return;
        }

        // Добавляем слушатель нажатий на точки.
        seriesDots.setOnDataPointTapListener (new OnDataPointTapListener () {
            @Override
            public void onTap (Series series, DataPointInterface dataPoint) {
                showInfoAlert (String.format (getResources ().getString (R.string.point_info_text).toString (), dataPoint.getX (), dataPoint.getY ()));
            }
        });

        // Настраиваем серию и добавляем на график.
        seriesDots.setDrawDataPoints (true);
        seriesDots.setDataPointsRadius (POINTS_RADIUS);
        graph.addSeries (seriesDots);

        // Задаем настройки графика.
        graph.getViewport ().setXAxisBoundsStatus (Viewport.AxisBoundsStatus.FIX);// Режим автомасштабирования графика - очень важно.
        graph.getViewport ().setYAxisBoundsStatus (Viewport.AxisBoundsStatus.FIX);
        graph.getViewport ().setScalable (true); // enables horizontal zooming and scrolling
    }

    //----------------------------------------------------------------------------------------------
    // Вспомгательные методы.
    //----------------------------------------------------------------------------------------------

    /**
     * Проверить поля на корректность, выполнить валидацию данных
     *
     * @return boolean Валидация пройдена?
     */
    protected boolean isEditTextsAreValid () {
        // Найдем элементы.
        EditText editTextDotsCount          = (EditText) findViewById (R.id.editTextDotsCount);
        EditText editTextDefinitionAreaFrom = (EditText) findViewById (R.id.editTextDefinitionAreaFrom);
        EditText editTextDefinitionAreaTo   = (EditText) findViewById (R.id.editTextDefinitionAreaTo);

        // Лист ошибок.
        ArrayList<String> errors = new ArrayList<> ();

        // Проверяем на пустоту.
        if (editTextDotsCount.getText ().toString ().isEmpty () || editTextDefinitionAreaFrom.getText ().toString ().isEmpty ()
                || editTextDefinitionAreaTo.getText ().toString ().isEmpty ()) {
            errors.add (getResources ().getString (R.string.error_empty_poles).toString ());

            showErrorsInAlert (errors);
            return false;
        }

        // Переводим  в числовые типы данных.
        int    dotsCount          = Integer.valueOf (editTextDotsCount.getText ().toString ());
        double definitionAreaFrom = Double.valueOf (editTextDefinitionAreaFrom.getText ().toString ());
        double definitionAreaTo   = Double.valueOf (editTextDefinitionAreaTo.getText ().toString ());

        // Проверяем корректность.
        if (dotsCount < DOTS_COUNT_MIN || dotsCount > DOTS_COUNT_MAX) {
            errors.add (String.format (getResources ().getString (R.string.error_wrong_dots_count).toString (), DOTS_COUNT_MIN, DOTS_COUNT_MAX));
        }
        if (definitionAreaFrom < DEFINITION_AREA_MIN || definitionAreaFrom > DEFINITION_AREA_MAX
                || definitionAreaTo < DEFINITION_AREA_MIN || definitionAreaTo > DEFINITION_AREA_MAX) {
            errors.add (String.format (getResources ().getString (R.string.error_wrong_definition_area).toString (), DEFINITION_AREA_MIN, DEFINITION_AREA_MAX));
        }
        if (definitionAreaFrom > definitionAreaTo) {
            errors.add (getResources ().getString (R.string.error_wrong_definition_area_2).toString ());
        }

        // Показываем ошибки валидации, если есть.
        if (!errors.isEmpty ()) {
            showErrorsInAlert (errors);
            return false;
        }

        return true;
    }

    /**
     * Показать в AlertDialog'е переденные ошибки.
     *
     * @param _errors Лист с ошибками.
     */
    protected void showErrorsInAlert (ArrayList<String> _errors) {
        String errorMsg = "";
        for (String error : _errors) {
            errorMsg += String.format (getResources ().getString (R.string.error_formatting), error);
        }
        showErrorAlert (getResources ().getString (R.string.errors_title) + errorMsg);
    }

    /**
     * Показать AlertDialog с ошибкой.
     *
     * @param _message Текст ошибки.
     */
    protected void showErrorAlert (String _message) {
        showDialog (getResources ().getString (R.string.dialog_title_error).toString (), _message);
    }

    /**
     * Показать информационный AlertDialog.
     *
     * @param _message
     */
    protected void showInfoAlert (String _message) {
        showDialog (getResources ().getString (R.string.dialog_title_info).toString (), _message);
    }

    /**
     * Показать AlertDialog.
     *
     * @param _title   Название окна.
     * @param _message Сообщение.
     */
    protected void showDialog (String _title, String _message) {
        // setup the alert builder
        AlertDialog.Builder builder = new AlertDialog.Builder (this);
        builder.setTitle (_title);
        builder.setMessage (_message);

        // add a button
        builder.setPositiveButton (getResources ().getString (R.string.dialog_button_text), null);

        // create and show the alert dialog
        AlertDialog dialog = builder.create ();
        dialog.show ();
    }
}
