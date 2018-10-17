package com.example4.user.testplottingapp4.Calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EmptyStackException;
import java.util.Stack;

/**
 * Класс калькулятора. Калькулятор со скобками.
 * <p>
 * !!!ВНИМАНИЕ!! Операторы не должны содеражать части названий друг друга.
 * Например, не допускается делать операторы вида "tan" и "ctan". ctan содержит tan. Так нельзя.
 * <p>
 * Чтобы добавить новый оператор, надо внести изменения в 4 места:
 * - REGEX_OPERATORS
 * - getPriority()
 * - isLeftAssociative()
 * - calculateReversePolishNotation()
 */
public class Calculator {
    /**
     * Регулярка для проверки числа типа double.
     */
    public static final String REGEX_DOUBLE = "^-?(0|[1-9][0-9]*)(\\.[0-9]+)*(E-?[1-9][0-9]*)?$";

    /**
     * Регулярка для определения операторов.
     */
    protected static String REGEX_OPERATORS = "^(\\+|-|\\*|/|\\^|±|sin|cos|tan|ctg|ln)$";
    //------------------------------------------------------------------------------------------------------------------

    /**
     * Исключение, которое бросает калькулятор при ошибках.
     */
    public static class CalculatorErrorException extends Exception {
        public CalculatorErrorException (String _msg) {
            super (_msg);
        }
    }

    /**
     * Исключение, когда результат вычисления равен "NaN".
     * Например, при делении на ноль или при возведении отрицательного числа в дробную степень.
     */
    public static class CalculatorNaNException extends Exception {
        public CalculatorNaNException (String _msg) {
            super (_msg);
        }
    }

    //------------------------------------------------------------------------------------------------------------------

    /**
     * Вычислить значение переданного выражения.
     *
     * @param _expression Выражение в нормальной форме.
     * @return
     * @throws Exception Бросает исключения в случае синтаксических ошибок или неправильности скобочной структуры.
     */
    public static double calculateNormal (String _expression) throws CalculatorErrorException, CalculatorNaNException {
        return calculateReversePolishNotation (convertInfixToRPN (_expression));
    }

    /**
     * Вычислить значение уравнения.
     *
     * @param _expressionNormal Строка с выражением. Может содержать символ "x".
     * @param _x                Значение параметра "x". Символ "x" из строки будет заменен на это значение.
     * @return
     * @throws CalculatorErrorException
     */
    public static double calculateNormalEquation (String _expressionNormal, double _x) throws CalculatorErrorException, CalculatorNaNException {
        _expressionNormal = _expressionNormal.replaceAll ("[xX]", "(" + _x + ")");

        return calculateReversePolishNotation (convertInfixToRPN (_expressionNormal));
    }

    //------------------------------------------------------------------------------------------------------------------
    // МЕТОДЫ КОНВЕРТИРОВАНИЯ INFIX TO RPN, А ТАКЖЕ ВЫЧИСЛЕНИЕ RPN.
    //------------------------------------------------------------------------------------------------------------------

    /**
     * Преобразовать инфиксную скобочную запись арифметического выражения в обратную польскую нотацию.
     *
     * @param _exp Выражение в обычной инфиксной скобочной форме..
     * @return Выражение в форме RPN.
     */
    public static String convertInfixToRPN (String _exp) throws CalculatorErrorException, CalculatorNaNException {
        // Стек. На нем будем вычислять.
        Stack<String> stack = new Stack<String> ();

        // Итоговая строка
        String result = "";

        String exp = formatExpToSystemView (_exp);
        System.out.println ("ToRPN: Выражение в инфиксной форме (User):   \"" + _exp + "\".");
        System.out.println ("ToRPN: Выражение в инфиксной форме (System): \"" + exp + "\".");
        if (!checkBracketsStructure (exp)) {
            throw new CalculatorErrorException ("ToRPN: Структура скобок неверна!");
        }

        // Проходимся по символам.
        String[] chars = exp.trim ().split ("\\s");
        for (String token : chars) {
            //----------------------------
            if (isNumber (token)) {
                result += token + " ";
            } else if (token.equals ("(")) {
                stack.push (token);
            } else if (token.equals (")")) {
                try {
                    while (!stack.peek ().equals ("(")) {
                        result += stack.pop () + " ";
                    }
                    stack.pop ();// Извлекаем открывающую скобку, не добавляя ее в выходную строку.
                } catch (EmptyStackException e) {
                    throw new CalculatorErrorException ("ToRPN: Синтаксическая ошибка при конвертации в RPN: неверный разделитель. Выражение: \"" + exp + "\".");
                }
            } else {
                while (!stack.isEmpty ()) {
                    // Если на вершине стека оператор и приоритет текущего оператора меньше чем топового
                    // - кладем топовый в вывод.
                    if (isOperator (stack.peek ()) &&
                            ((isLeftAssociative (token) && (getPriority (token) <= getPriority (stack.peek ())))
                                    || (!isLeftAssociative (token) && (getPriority (token) < getPriority (stack.peek ()))))) {
                        // Кладем топовый оператор в вывод.
                        result += stack.pop () + " ";
                    } else {
                        break;
                    }
                }

                // Помещаем текущий оператор в стек.
                stack.push (token);
            }
        }

        // Выталкиваем оставшиеся элементы из стека.
        String stackLost = "";
        while (!stack.isEmpty ()) {
            stackLost += stack.pop () + " ";
        }
        result += stackLost;

//        System.out.println ("ToRPN: Проверка. Стек:                       \"" + stackLost + "\".");
        System.out.println ("ToRPN: Результат в ОПН:                      \"" + result + "\".");
        System.out.println ("ToRPN: Результат вычисления ОПН:             \"" + calculateReversePolishNotation (result) + "\".");
//        System.out.println ();

        return result;
    }

    /**
     * Выполнить вычисление в Обратной Польской Нотации.
     *
     * @param _exp
     * @return Результат вычисления.
     */
    public static double calculateReversePolishNotation (String _exp) throws CalculatorErrorException, CalculatorNaNException {
        // Стек. На нем будем вычислять.
        Stack<Double> stack = new Stack<Double> ();

        // Грузим символы.
        String[] chars = _exp.split ("\\s");

        for (String ch : chars) {
            double op1;
            double op2;

            try {
                switch (ch) {
                    // Бинарные операторы:
                    case "+":
                        op1 = stack.pop ();
                        op2 = stack.pop ();
                        stack.push (op2 + op1);
                        break;
                    case "-":
                        op1 = stack.pop ();
                        op2 = stack.pop ();
                        stack.push (op2 - op1);
                        break;
                    case "*":
                        op1 = stack.pop ();
                        op2 = stack.pop ();
                        stack.push (op1 * op2);
                        break;
                    case "/":
                        op1 = stack.pop ();
                        op2 = stack.pop ();
                        stack.push (op2 / op1);
                        break;

                    // Унарные операторы:
                    case "^":
                        op1 = stack.pop ();
                        op2 = stack.pop ();
                        stack.push (Math.pow (op2, op1));
                        break;
                    case "±":
                        op1 = stack.pop ();
                        stack.push (-1 * op1);
                        break;
                    case "sin":
                        op1 = stack.pop ();
                        stack.push (Math.sin (op1));
                        break;
                    case "cos":
                        op1 = stack.pop ();
                        stack.push (Math.cos (op1));
                        break;
                    case "tan":
                        op1 = stack.pop ();
                        stack.push (Math.tan (op1));
                        break;
                    case "ctg":
                        op1 = stack.pop ();
                        stack.push (1.0 / Math.tan (op1));
                        break;
                    case "ln":
                        op1 = stack.pop ();
                        stack.push (Math.log (op1));
                        break;

                    default:
                        if (!isNumber (ch)) {
                            throw new CalculatorErrorException ("CalculateRPN: \"" + ch + "\" - неверный формат операнда!");
                        } else {
                            stack.push (Double.valueOf (ch));
                        }
                        break;
                }
            } catch (EmptyStackException e) {
                throw new CalculatorErrorException ("CalculateRPN: Синтаксическая ошибка при вычислении RPN: неверный разделитель. Выражение: \"" + _exp + "\".");
            }
        }

        double result = stack.pop ();

        if (String.valueOf (result).equals ("NaN") || String.valueOf (result).equals ("Infinity") || String.valueOf (result).equals ("-Infinity")) {
            throw new CalculatorNaNException ("CalculateRPN: Математическая ошибка. Результат равен \"NaN/Infinity/-Infinity \".");
        }

        return result;
    }
    //------------------------------------------------------------------------------------------------------------------

    /**
     * Получить приоритет оператора.
     * <p>
     * Операторы:
     * Приоритет Оператор Ассоциативность
     * 4 ! правая
     * 3 * / % левая
     * 2 + - левая
     * 1 = левая
     *
     * @param _op
     * @return
     */
    protected static int getPriority (String _op) {
        switch (_op) {
            case "±":
                return 5;
            case "!":
            case "^":
            case "sin":
            case "cos":
            case "tan":
            case "ctg":
            case "ln":
                return 4;
            case "*":
            case "/":
                return 3;
            case "+":
            case "-":
                return 2;
            case "(":
            case ")":
                return 1;
            default:
                return -1;
        }
    }

    protected static boolean isLeftAssociative (String _op) {
        switch (_op) {
            case "±":
            case "!":
            case "^":
            case "sin":
            case "cos":
            case "tan":
            case "ctg":
            case "ln":
                return false;
            default:
            case "*":
            case "/":
            case "+":
            case "-":
                return true;
        }
    }

    /**
     * Проверить правильность скобочной структуры.
     *
     * @param _exp Выражение со скобками.
     * @return
     */
    protected static boolean checkBracketsStructure (String _exp) {
        int number = -1;
        int a = 0;

        while (++number < _exp.length ()) {
            String ch = String.valueOf (_exp.charAt (number));
            if (ch.equals ("(")) {
                ++a;
            } else if (ch.equals (")") && --a < 0) {
                break;
            }
        }

        return (a == 0);
    }


    //------------------------------------------------------------------------------------------------------------------

    /**
     * Является ли эта строка числом? Точнее, является ли она допустимым оператором.
     *
     * @param _str
     * @return
     */
    protected static boolean isNumber (String _str) {
        return _str.matches (REGEX_DOUBLE);
    }

    /**
     * Является ли эта строка оператором?
     *
     * @param _str
     * @return
     */
    protected static boolean isOperator (String _str) {
        return _str.matches (REGEX_OPERATORS);
    }

    /**
     * Округлить число до заданных знаков после запятой.
     *
     * @param _num
     * @param _scale
     * @return
     */
    protected static double round (double _num, int _scale) {
        BigDecimal bd = new BigDecimal (_num).setScale (_scale, RoundingMode.HALF_UP);
        return bd.doubleValue ();
    }

    //------------------------------------------------------------------------------------------------------------------

    /**
     * Форматировать выражение в системный вид - с пробельным разделением всех операторов и операндов.
     *
     * @param _exp
     * @return
     */
    public static String formatExpToSystemView (String _exp) {
        return prepareOperatorsInString (_exp)
                .replaceAll ("([0-9]+)\\s*E\\s*-\\s*([0-9]+)", "$1E-$2")

                .replaceAll ("\\s*([^ 0-9E\\)]+)\\s*-", " $1 ± ")
                .replaceAll ("^\\s*-\\s*([^-]+)", " ± $1 ")

                .replaceAll ("\\s*\\(\\s*", " ( ")
                .replaceAll ("\\s*\\)\\s*", " ) ")
                .replaceAll ("\\s+", " ")
                .replaceAll ("E\\s*-", "E-")
                .trim ();
    }

    /**
     * Расставить пробелы вокруг всех встреченных в строке операторов.
     *
     * @param _exp
     * @return
     */
    protected static String prepareOperatorsInString (String _exp) {
        String result = _exp;
        String[] ops = REGEX_OPERATORS.substring (2, REGEX_OPERATORS.length () - 2).split ("\\|");

        for (String op : ops) {
            result = result.replaceAll ("\\s*" + op + "\\s*", " " + op + " ");
        }

        return result;
    }

    public static String formatExpToBeautifulView (String _exp) {
        return formatExpToSystemView (_exp)
                .replaceAll ("\\s*\\(\\s*", " (")
                .replaceAll ("\\s*\\)\\s*", ") ")

                .replaceAll ("\\)\\s*\\)", "))")
                .replaceAll ("\\(\\s*\\(", "((")

                .replaceAll ("±\\s*", "-");
    }
}
