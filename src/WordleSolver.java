import java.io.*;
import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

class WordleSolver {

    static final int wordSize = 5;
    static final String wordsFilename = "words_en.txt";

    public static void main(String[] args) {
        List<String> filteredWords;
        String filteredWordsFilename = wordsFilename.substring(0, wordsFilename.lastIndexOf('.')) + "_filtered.txt";
        File filteredWordsFile = new File(filteredWordsFilename);
        if (filteredWordsFile.exists()) {
            filteredWords = retrieveWordsFromFile(filteredWordsFilename);
        } else {
            System.out.println("Analyzing words input file... (this process is made only once)");
            filteredWords = obtainFilteredWordsFromFile(wordsFilename, wordSize);
            createWordsFile(filteredWords, filteredWordsFilename);
            int[] occurrences = obtainOccurrences(filteredWords);
            printLettersByOccurrence(occurrences);
            printBestStartingWord(filteredWords, occurrences);
            printBestStartingWordPair(filteredWords, occurrences);
            System.out.println();
        }

        Scanner scanner = new Scanner(System.in);
        List<String> candidates = List.of("", ""); // Placeholder value to avoid adding an extra check to the loop
        Set<String> forbiddenLetters = new HashSet<>();
        Set<String> lookaheadLetters = new HashSet<>();
        List<Set<String>> yellowLetterSets = Stream.generate(HashSet<String>::new).limit(wordSize)
                .collect(Collectors.toList());
        StringBuilder knownLetters = new StringBuilder("*".repeat(wordSize));
        System.out.println("### Input format: \"chair _y__g\" (without quotes), " +
                "where '_' represents a gray letter, 'y' a yellow letter and 'g' a green letter.");
        for (int n = 1; candidates.size() > 1; n++) {

            System.out.print("Enter " + getOrdinal(n) + " word: ");
            String input = scanner.nextLine().toLowerCase();
            while (!input.matches(String.format("[a-z]{%1$d} [_yg]{%1$d}", wordSize))) {
                System.out.println("Wrong format, look at the format instructions above and try again.");
                System.out.print("Enter " + getOrdinal(n) + " word: ");
                input = scanner.nextLine().toLowerCase();
            }

            String[] word = input.split(" ");

            IntStream.range(0, wordSize).filter(i -> word[1].charAt(i) == '_')
                    .forEach(i -> forbiddenLetters.add(Character.toString(word[0].charAt(i))));
            String forbiddenString = String.join("", forbiddenLetters);

            IntStream.range(0, wordSize).filter(i -> word[1].charAt(i) == 'y')
                    .forEach(i -> {
                        String c = Character.toString(word[0].charAt(i));
                        lookaheadLetters.add(c);
                        yellowLetterSets.get(i).add(c);
                    });
            String lookaheadString = lookaheadLetters.stream().reduce("", (str, c) -> str + "(?=.*" + c + ")");

            IntStream.range(0, wordSize).filter(i -> word[1].charAt(i) == 'g')
                    .forEach(i -> knownLetters.setCharAt(i, word[0].charAt(i)));

            StringBuilder regex = new StringBuilder(knownLetters);
            IntStream.range(0, wordSize).boxed().sorted(Collections.reverseOrder()).filter(i -> regex.charAt(i) == '*')
                    .forEach(i -> regex.replace(i, i + 1, String.format("[^ %s%s]", forbiddenString,
                            String.join("", yellowLetterSets.get(i)))));
            regex.insert(0, lookaheadString);

            candidates = filteredWords.stream().filter(w -> w.matches(regex.toString())).toList();
            candidates.forEach(System.out::println);
            System.out.println(candidates.size() +
                    " possible solutions found (the secret word is usually the most common word).");
            System.out.println();
        }
    }

    static List<String> obtainFilteredWordsFromFile(String filename, int wordSize) {
        List<String> filteredWords = new ArrayList<>();
        try {
            Scanner fileScanner = new Scanner(new File(filename));
            while (fileScanner.hasNextLine()) {
                String word = fileScanner.nextLine();
                if (word.length() == wordSize) {
                    word = Normalizer.normalize(word.toLowerCase(), Normalizer.Form.NFD);
                    word = word.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
                    if (word.matches("^[a-z]+$")) {
                        filteredWords.add(word);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return filteredWords.stream().distinct().toList();
    }

    static List<String> retrieveWordsFromFile(String filename) {
        List<String> words = new ArrayList<>();
        try {
            Scanner fileScanner = new Scanner(new File(filename));
            while (fileScanner.hasNextLine()) {
                words.add(fileScanner.nextLine());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return words;
    }

    static void createWordsFile(Collection<String> words, String filename) {
        try (FileWriter fileWriter = new FileWriter(filename);
             PrintWriter printWriter = new PrintWriter(fileWriter)) {
            words.forEach(printWriter::println);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static int[] obtainOccurrences(List<String> filteredWords) {
        int[] occurrences = new int[26];
        filteredWords.forEach(word -> word.chars().forEach(c -> occurrences[c - 'a']++));
        return occurrences;
    }

    static void printLettersByOccurrence(int[] occurrences) {
        int letterCount = IntStream.of(occurrences).sum();
        IntStream.range(0, occurrences.length)
                .mapToObj(i -> new AbstractMap.SimpleEntry<>('a' + i, 100f * occurrences[i] / letterCount))
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .forEach(entry -> System.out.printf("%c: %.2f%%%n", entry.getKey(), entry.getValue()));
    }

    static void printBestStartingWord(List<String> words, int[] occurrences) {
        String bestWord = words.stream()
                .map(word -> new AbstractMap.SimpleEntry<>(word, getWordValue(word, occurrences)))
                .max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("");
        System.out.println("Best starting word: " + bestWord);
    }

    static void printBestStartingWordPair(List<String> words, int[] occurrences) {
        System.out.print("Best starting word pair (may take a while...): ");
        AbstractMap.SimpleEntry<String, Integer> bestWord = new AbstractMap.SimpleEntry<>("", 0);
        for (int i = 0; i < words.size(); i++) {
            for (int j = i + 1; j < words.size(); j++) {
                int wordValue = getWordValue(words.get(i) + words.get(j), occurrences);
                if (wordValue > bestWord.getValue()) {
                    bestWord = new AbstractMap.SimpleEntry<>(words.get(i) + "," + words.get(j), wordValue);
                } else if (wordValue == bestWord.getValue()) {
                    int new1stWordValue = getWordValue(words.get(i), occurrences);
                    int new2ndWordValue = getWordValue(words.get(j), occurrences);
                    int old1stWordValue = getWordValue(bestWord.getKey().split(",")[0], occurrences);
                    int old2ndWordValue = getWordValue(bestWord.getKey().split(",")[1], occurrences);
                    if (Math.max(new1stWordValue, new2ndWordValue) > Math.max(old1stWordValue, old2ndWordValue)) {
                        bestWord = new AbstractMap.SimpleEntry<>(words.get(i) + "," + words.get(j), wordValue);
                    }
                }
            }
        }
        String[] wordPair = bestWord.getKey().split(",");
        boolean firstIsBest = getWordValue(wordPair[0], occurrences) > getWordValue(wordPair[1], occurrences);
        System.out.println(firstIsBest ? (wordPair[0] + ", " + wordPair[1]) : (wordPair[1] + ", " + wordPair[0]));
    }

    static int getWordValue(String word, int[] occurrences) {
        return IntStream.range(0, occurrences.length)
                .filter(i -> word.indexOf('a' + i) >= 0)
                .map(i -> occurrences[i]).sum();
    }

    static String getOrdinal(int i) {
        return switch (i) {
            case 1 -> "1st";
            case 2 -> "2nd";
            case 3 -> "3rd";
            default -> i + "th";
        };
    }
}
