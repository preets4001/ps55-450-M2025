package M3;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/*
Challenge 3: Mad Libs Generator (Randomized Stories)
-----------------------------------------------------
- Load a **random** story from the "stories" folder
- Extract **each line** into a collection (i.e., ArrayList)
- Prompts user for each placeholder (i.e., <adjective>) 
    - Any word the user types is acceptable, no need to verify if it matches the placeholder type
    - Any placeholder with underscores should display with spaces instead
- Replace placeholders with user input (assign back to original slot in collection)
*/

public class MadLibsGenerator extends BaseClass {
    private static final String STORIES_FOLDER = "M3/stories";
    private static String ucid = "ps55"; // <-- change to your ucid

    public static void main(String[] args) {
        printHeader(ucid, 3,
                "Objective: Implement a Mad Libs generator that replaces placeholders dynamically.");

        Scanner scanner = new Scanner(System.in);
        File folder = new File(STORIES_FOLDER);

        if (!folder.exists() || !folder.isDirectory() || folder.listFiles().length == 0) {
            System.out.println("Error: No stories found in the 'stories' folder.");
            printFooter(ucid, 3);
            scanner.close();
            return;
        }
        File[] storyFiles = folder.listFiles();
        File selectedStory = storyFiles[new Random().nextInt(storyFiles.length)];

        List<String> lines = new ArrayList<>();

        try (Scanner fileScanner = new Scanner(selectedStory)) {
            while (fileScanner.hasNextLine()) {
                lines.add(fileScanner.nextLine());
            }
        } catch (FileNotFoundException e) {
            System.out.println("Error reading story file.");
            printFooter(ucid, 3);
            scanner.close();
            return;
        }

        // Pattern to match placeholders like <noun> or <verb_past>
        Pattern placeholderPattern = Pattern.compile("<(.*?)>");

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            Matcher matcher = placeholderPattern.matcher(line);
            StringBuffer newLine = new StringBuffer();

            while (matcher.find()) {
                String placeholder = matcher.group(1);
                String readablePlaceholder = placeholder.replace("_", " ");
                System.out.print("Enter a " + readablePlaceholder + ": ");
                String userInput = scanner.nextLine();
                matcher.appendReplacement(newLine, Matcher.quoteReplacement(userInput));
            }
            matcher.appendTail(newLine);
            lines.set(i, newLine.toString());
        }

        // Print completed story
        System.out.println("\nYour Completed Mad Libs Story:\n");
        for (String line : lines) {
            System.out.println(line);
        }

        printFooter(ucid, 3);
        scanner.close();
    }
}