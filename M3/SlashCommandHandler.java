package M3;
import java.util.Random;
import java.util.Scanner;

public class SlashCommandHandler extends BaseClass {
    private static String ucid = "ps55"; // <-- change to your UCID

 public static void main(String[] args) {
        printHeader(ucid, 2, "Objective: Implement a simple slash command parser.");

        Scanner scanner = new Scanner(System.in);
        Random rand = new Random();

        while (true) {
            System.out.print("Enter command: ");
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                System.out.println("Please enter a command.");
                continue;
            }

            String lowerInput = input.toLowerCase();

            // /quit
            if (lowerInput.equals("/quit")) {
                System.out.println("Exiting program.");
                break;
            }

            // /greet <name>
            if (lowerInput.startsWith("/greet ")) {
                String[] parts = input.split("\\s+", 2);
                if (parts.length == 2 && !parts[1].isEmpty()) {
                    System.out.println("Hello, " + parts[1] + "!");
                } else {
                    System.out.println("Error: /greet requires a name.");
                }
                continue;
            }

            // /roll <num>d<sides>
            if (lowerInput.startsWith("/roll ")) {
                String[] parts = input.split("\\s+", 2);
                if (parts.length == 2) {
                    String[] diceParts = parts[1].toLowerCase().split("d");
                    if (diceParts.length == 2) {
                        try {
                            int numDice = Integer.parseInt(diceParts[0]);
                            int sides = Integer.parseInt(diceParts[1]);

                            if (numDice <= 0 || sides <= 0) {
                                System.out.println("Error: Numbers must be positive.");
                                continue;
                            }

                            int total = 0;
                            for (int i = 0; i < numDice; i++) {
                                total += rand.nextInt(sides) + 1;
                            }
                            System.out.println("Rolled " + numDice + "d" + sides + " and got " + total + "!");
                        } catch (NumberFormatException e) {
                            System.out.println("Error: Invalid dice format. Use /roll <num>d<sides>.");
                        }
                    } else {
                        System.out.println("Error: Invalid roll format. Use /roll <num>d<sides>.");
                    }
                } else {
                    System.out.println("Error: /roll requires an argument like 2d6.");
                }
                continue;
            }

            // /echo <message>
            if (lowerInput.startsWith("/echo ")) {
                String[] parts = input.split("\\s+", 2);
                if (parts.length == 2 && !parts[1].isEmpty()) {
                    System.out.println(parts[1]);
                } else {
                    System.out.println("Error: /echo requires a message.");
                }
                continue;
            }

            // If no command matched
            System.out.println("Error: Unknown or unsupported command.");
        }

        printFooter(ucid, 2);
        scanner.close();
    }
}