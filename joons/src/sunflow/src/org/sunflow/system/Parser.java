package org.sunflow.system;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class Parser {

    private FileReader file;
    private BufferedReader bf;
    private String[] lineTokens;
    private int index;

    public Parser(String filename) throws FileNotFoundException {
        file = new FileReader(filename);
        bf = new BufferedReader(file);
        lineTokens = new String[0];
        index = 0;
    }

    public void close() throws IOException {
        if (file != null) {
            file.close();
        }
        bf = null;
    }

    public String getNextToken() throws IOException {
        while (true) {
            String tok = fetchNextToken();
            if (tok == null) {
                return null;
            }
            if (tok.equals("/*")) {
                do {
                    tok = fetchNextToken();
                    if (tok == null) {
                        return null;
                    }
                } while (!tok.equals("*/"));
            } else {
                return tok;
            }
        }
    }

    public boolean peekNextToken(String tok) throws IOException {
        while (true) {
            String t = fetchNextToken();
            if (t == null) {
                return false; // nothing left
            }
            if (t.equals("/*")) {
                do {
                    t = fetchNextToken();
                    if (t == null) {
                        return false; // nothing left
                    }
                } while (!t.equals("*/"));
            } else if (t.equals(tok)) {
                // we found the right token, keep parsing
                return true;
            } else {
                // rewind the token so we can try again
                index--;
                return false;
            }
        }
    }

    private String fetchNextToken() throws IOException {
        if (bf == null) {
            return null;
        }
        while (true) {
            if (index < lineTokens.length) {
                return lineTokens[index++];
            } else if (!getNextLine()) {
                return null;
            }
        }
    }

    private boolean getNextLine() throws IOException {
        String line = bf.readLine();

        if (line == null) {
            return false;
        }

        ArrayList<String> tokenList = new ArrayList<String>();
        StringBuilder current = new StringBuilder(80);
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (current.length() == 0 && (c == '%' || c == '#')) {
                break;
            }

            boolean quote = c == '\"';
            inQuotes = inQuotes ^ quote;

            if (!quote && (inQuotes || !Character.isWhitespace(c))) {
                current.append(c);
            } else if (current.length() > 0) {
                tokenList.add(current.toString());
                current = new StringBuilder(80);
            }
        }

        if (current.length() > 0) {
            tokenList.add(current.toString());
        }
        lineTokens = tokenList.toArray(new String[0]);
        index = 0;
        return true;
    }

    public String getNextCodeBlock() throws ParserException, IOException {
        // read a java code block
        StringBuilder code = new StringBuilder(80);
        checkNextToken("<code>");
        while (true) {
            String line = bf.readLine();
            if (line.trim().equals("</code>")) {
                return code.toString();
            }
            code.append(line).append("\n");
        }
    }

    public boolean getNextBoolean() throws IOException {
        return Boolean.valueOf(getNextToken()).booleanValue();
    }

    public int getNextInt() throws IOException {
        return Integer.parseInt(getNextToken());
    }

    public float getNextFloat() throws IOException {
        return Float.parseFloat(getNextToken());
    }

    public void checkNextToken(String token) throws ParserException, IOException {
        String found = getNextToken();
        if (!token.equals(found)) {
            close();
            throw new ParserException(token, found);
        }
    }

    @SuppressWarnings("serial")
    public static class ParserException extends Exception {

        private ParserException(String token, String found) {
            super(String.format("Expecting %s found %s", token, found));
        }
    }
}