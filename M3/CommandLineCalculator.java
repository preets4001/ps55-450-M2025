package M3;

/*
Challenge 1: Command-Line Calculator
------------------------------------
- Accept two numbers and an operator as command-line arguments
- Supports addition (+) and subtraction (-)
- Allow integer and floating-point numbers
- Ensures correct decimal places in output based on input (e.g., 0.1 + 0.2 → 1 decimal place)
- Display an error for invalid inputs or unsupported operators
- Capture 5 variations of tests
*/

public class CommandLineCalculator extends BaseClass {
    private static String ucid = "ps55"; // <-- change to your ucid

    public static void main(String[] args) {
        printHeader(ucid, 1, "Objective: Implement a calculator using command-line arguments.");

        if (args.length != 3) {
            System.out.println("Usage: java M3.CommandLineCalculator <num1> <operator> <num2>");
            printFooter(ucid, 1);
            return;
        }

         try {
            String num1Str = args[0];
            String operator = args[1];
            String num2Str = args[2];

            if (!operator.equals("+") && !operator.equals("-")) {
                System.out.println("Error: Only + and - operators are supported.");
                printFooter(ucid, 1);
                return;
            }

            int decimalPlaces = Math.max(getDecimalPlaces(num1Str), getDecimalPlaces(num2Str));
            DecimalFormat formatter = new DecimalFormat(getDecimalFormat(decimalPlaces));

            double num1 = Double.parseDouble(num1Str);
            double num2 = Double.parseDouble(num2Str);
            double result = operator.equals("+") ? num1 + num2 : num1 - num2;

            System.out.println("Result: " + formatter.format(result));
        } catch (NumberFormatException e) {
            System.out.println("Invalid number format. Please enter valid numbers.");
        } catch (Exception e) {
            System.out.println("Unexpected error: " + e.getMessage());
        }

        printFooter(ucid, 1);
    }

    private static int getDecimalPlaces(String num) {
        if (num.contains(".")) {
            return num.length() - num.indexOf('.') - 1;
        }
        return 0;
    }

    private static String getDecimalFormat(int decimalPlaces) {
        return decimalPlaces == 0 ? "0" : "0." + repeatZeros(decimalPlaces);
    }

    private static String repeatZeros(int count) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < count; i++) {
        sb.append("0");
    }
    return sb.toString();
}
}