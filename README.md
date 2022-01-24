# Wordle Solver
Solve Wordle puzzles effortlessly. Compute the best words to start with any given language.

## Usage
1. Compile and run <code>WordleSolver.java</code> with any tool supporting Java version 16+ and follow the instructions displayed in the console.

2. (Optional) Change <code>wordSize</code> if your Wordle game uses word sizes other than 5.

3. (Optional) Change <code>wordsFilename</code> if your Wordle game uses a language other than English. This project comes bundled with text files containing all English and Spanish words. You can add support for any other language by obtaining a text file containing all its words and changing <code>wordsFilename</code> accordingly.

The first time the program is run for any given <code>wordsFilename</code> will generate a filtered version of the file (to speed up subsequent queries) and display useful information (letter frequency and best words to start with).

The user is expected to input each word introduced in the game in the format "chair \_y\_\_g" where:
- 'chair' is the word introduced.
- '\_' means the letter in that position is **gray** (not in the secret word).
- 'y' means the letter in that position is **yellow** (in the secret word but in the wrong spot).
- 'g' means the letter in that position is **green** (in the secret word and in the correct spot).
