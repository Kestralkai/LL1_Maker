package parser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class LexicalAnalyzer {
    
    int state = 0;
    int commentLvl = 0;
    int commentState = 0;
    int symbolState = 0;
    int saveState = 0;
    String curToken = "";
    boolean error = false;
    List<Token> tokenList = new ArrayList<>();
    
    public static List<Token> getTokenList(String fileName) {
        
        System.out.println();
        System.out.println("-------------------------------------------------");
        System.out.println("             Begin lexical analysis              ");
        System.out.println("-------------------------------------------------");
        
        LexicalAnalyzer analyzer = new LexicalAnalyzer();
        
        try {
            
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            String lineToRead;
            while((lineToRead = br.readLine()) != null) {
                System.out.println("\nINPUT: " + lineToRead);
                char[] charArray = lineToRead.toCharArray();                
                for(char c : charArray)
                    analyzer.read(c);
                analyzer.newLine();
            }
            
        } catch(IOException | NumberFormatException e) {
            System.out.println(e.toString());
            System.exit(1);
        }
        
        System.out.println();
        System.out.println("-------------------------------------------------");
        System.out.println("              End lexical analysis               ");
        System.out.println("-------------------------------------------------");
        System.out.println();
        
        if(!analyzer.error)
            return analyzer.tokenList;
        else {
            System.out.println("Error detected during lexical analysis.");
            System.out.println("File cannot be parsed.");
            return null;
        }
        
    }
    
    private void newLine() {
        logToken();
        state = 0;
    }
    
    private void read(char c) {

        //System.out.print("[" + state + "] : " + c + "\n");
        //System.out.print("[" + state + "] : " + curToken + "\n");    
        if((c >= 'a' && c <= 'z') || (c  >= 'A' && c <= 'Z')) {
            symbolState = 0;
            if(c == 'E' && (state == 22 || state == 20))
                setState(23);
            else if(state == 0 || state == 4)
                setState(1);
            else if(state != 1)
                setState(4);
        }
        
        else if(c >= '0' && c <= '9') {
            symbolState = 0;
            if(state == 21 || state == 24)
                setState(state + 1);
            else if(state == 23)
                setState(25);
            else if(state == 0)
                setState(20);
            else if(state == 20 || state == 22 || state == 25)
                setState(state);
            else
                setState(4);
        }
        
        else
            switch(c) {
                case '*':
                    if(symbolState == 2) {
                        ++commentLvl;
                        symbolState = 0;
                        curToken = "";
                    }
                    else if(commentLvl > 0){
                        symbolState = 1;
                        setState(0);
                    }
                    else {
                        setState(0);
                        symbolState = 0;
                    }
                    break;
                case '/':
                    if(symbolState == 1 && commentLvl > 0) {
                        --commentLvl;
                        symbolState = -1;
                    }
                    else if(symbolState == 2) {
                        curToken = "";
                        setState(-1);
                    }
                    else {
                        symbolState = 2;
                        setState(0);
                    }
                    break;
                case '<':
                    symbolState = 3;
                    break;
                case '>':
                    symbolState = 4;
                    break;
                case '=':
                    if(symbolState >= 3 && symbolState <= 6) {
                        if(symbolState == 6)
                            state = 0;
                        symbolState = 0;
                    }
                    else {
                        setState(0);
                        symbolState = 5;
                    }
                    break;
                case '!':
                    symbolState = 6;
                    setState(4);
                    break;
                case '+':
                case '-':
                    if(state == 23)
                        setState(state + 1);
                    else
                        setState(0);
                    break;
                case ';':
                case ',':
                case '(':
                case ')':
                case '[':
                case ']':
                case '{':
                case '}':
                    setState(0);
                    symbolState = 0;
                    break;
                case ' ':
                case '\t':
                    break;
                case '.':
                    symbolState = 0;
                    if(state == 20)
                        setState(21);
                    else
                        setState(4);
                    break;
                default:
                    symbolState = 0;
                    setState(4);
                    break;
            }

        if(c == ' ' || c == '\t') {
            setState(0);
            symbolState = 0;
        }
        else if(state != -1 && commentLvl == 0 && symbolState >= 0) {
            curToken += c;
        }
        
    }
    
    private void setState(int s) {
        if(commentLvl > 0)
            return;
        switch(state) {
            case -1:
                break;
            case 0:
                logToken();
                state = s;
                break;
            case 1:
                if(s != 1) {
                    logToken();
                    if(s == 0)
                        state = s;
                    else {
                        state = 4;
                    }
                }
                else
                    state = s;
                break;
            case 20:
                if(s != 20 && s != 21 && s != 23) {
                    logToken();
                    if(s == 0)
                        state = s;
                    else
                        state = 4;
                }
                else
                    state = s;
                break;
            case 21:
                if(s != 22) {
                    logToken();
                    state = 4;
                }
                else
                    state = s;
                break;
            case 22:
                if(s != 22 && s != 23) {
                    logToken();
                    if(s == 0)
                        state = s;
                    else
                        state = 4;
                }
                else
                    state = s;
                break;
            case 23:
                if(s != 24 && s != 25) {
                    curToken = curToken.substring(0, curToken.length() - 1);
                    logToken();
                    curToken = "E";
                    state = 4;
                    if(s == 0) {
                        logToken();
                        state = 0;
                    }
                }
                else
                    state = s;
                break;
            case 24:
                if(s != 25) {
                    String tempToken = curToken.substring(
                            curToken.length() - 1, curToken.length());
                    curToken = curToken.substring(0, curToken.length() - 2);
                    logToken();
                    curToken = "E";
                    state = 4;
                    logToken();
                    curToken = tempToken;
                    state = 0;
                    logToken();
                }
                state = s;
                break;
            case 25:
                if(s != 25)
                    logToken();
                if(s == 0 || s == 25)
                    state = s;
                else
                    state = 4;
                break;
            case 4:
                if(s == 0) {
                    logToken();
                    state = s;
                }
                break;
        }
        
    }
    
    private void logToken() {
        curToken = curToken.trim();
        if(curToken.equals(""))
            return;
        if(state == 1) {
            if(curToken.equals("else") || curToken.equals("if") ||
                    curToken.equals("int") || curToken.equals("return") ||
                    curToken.equals("void") || curToken.equals("while") ||
                    curToken.equals("float")) {
                tokenList.add(new Token(curToken, curToken));
                System.out.print("keyword: ");
            } else {
                tokenList.add(new Token(curToken, "ID"));
                System.out.print("ID: ");
            }
        } else if(state >= 20 && state <= 25) {
            tokenList.add(new Token(curToken, "NUM"));
            System.out.print("NUM: ");
        } else if(state == 4) {
            error = true;
            System.out.print("Error: ");
        } else
            tokenList.add(new Token(curToken, curToken));
        System.out.println(curToken);
        curToken = "";
    }
    
}