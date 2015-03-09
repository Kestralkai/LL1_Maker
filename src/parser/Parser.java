/*
*   Project 2
*   Construction of Language Translators (COP5625)
*   Author: Timothy Deal, n00594817
*   Last Modified: 02/03/15
*/
package parser;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

class Parser {
    
    static final String DEFAULT_GRAMMAR_FILE = "cMinusGrammar.txt";
    
    public static SymbolList SYMBOLS;

    public static void main(String[] args) {
        System.out.println();
        if(args.length != 1 && args.length != 2) {
            System.out.println("Error - invalid number of arguements.\n");
            System.exit(1);
        }
        SYMBOLS = new SymbolList();
        Grammar grammar;
        if(args.length == 1)
            grammar = new Grammar(DEFAULT_GRAMMAR_FILE);
        else
            grammar = new Grammar(args[1]);
        grammar.makeLL1();
        
        if(!grammar.firstAndFollowCheck()) {
            System.out.println("Grammar is not LL(1)\n");
            System.exit(1);
        }
        Parser parser = new Parser(grammar);
        parser.constructParseTable();
        List<Token> tokenList = LexicalAnalyzer.getTokenList(args[0]);
        if(tokenList == null)
            System.exit(0);
        if(!parser.parseTokenList(tokenList)) {
            System.out.println("Unable to parse file.\n");
            System.exit(0);
        }
        System.out.println("\n\nFile parsed successfully!\n\n");
        
    }
    
    Symbol[][][] parseTable;
    Grammar grammar;
    
    Parser(Grammar g) {
        this.grammar = g;
    }
    
    public boolean parseTokenList(List<Token> tokenList) {
        if(tokenList == null)
            return true;
        tokenList.add(new Token("$",SYMBOLS.TERMINATE));
        Stack stack = new Stack();
        stack.push(SYMBOLS.START);
        int tokenIndex = 0;
        System.out.println("Top of parse stack:\n");
        while(!stack.isEmpty()) {
            System.out.println(((Symbol)stack.peek()).string);
            Symbol s = (Symbol)stack.pop();
            Symbol curToken = tokenList.get(tokenIndex).tokenType;
            if(s.isTerminal) {
                if(s.equals(SYMBOLS.EPSILON));
                else if(!s.equals(curToken)) {
                    System.out.println();
                    System.out.println("PARSE ERROR:");
                    System.out.println("Stack shows:   " + s.string);
                    System.out.println("Current token: " + curToken.string);
                    System.out.println();
                    return false;
                }
                else {
                    tokenIndex++;
                }
            } else {
                int x = SYMBOLS.NON_TERMINALS.indexOf(s);
                int y = SYMBOLS.TERMINALS.indexOf(curToken);
                if(parseTable[x][y] == null) {
                    System.out.println();
                    System.out.println("PARSE ERROR:");
                    System.out.println("Stack shows:   " + s.string);
                    System.out.println("Current token: " + curToken.string);
                    System.out.println();
                    return false;
                }
                for(int i = parseTable[x][y].length - 1; i >= 0; i--)
                    stack.push(parseTable[x][y][i]);
            }
        }
        return tokenList.get(tokenIndex).tokenType.string.equals("$");
    }
    
    private void constructParseTable() {
        parseTable = new Symbol
                [SYMBOLS.NON_TERMINALS.size()][SYMBOLS.TERMINALS.size()][];
        for(int i = 0; i < SYMBOLS.NON_TERMINALS.size(); i++) {
            Symbol nonTerm = SYMBOLS.NON_TERMINALS.get(i);
            List<GrammarRule> gRules = grammar.getRules(nonTerm);
            for (GrammarRule gRule : gRules) {
                List<Symbol> rule = gRule.RHS;
                List<Symbol> gRuleFirstSet = new ArrayList<>();
                SYMBOLS.firstSetMaker(gRule, gRuleFirstSet);
                for(Symbol s : gRuleFirstSet) {
                    if(s.equals(SYMBOLS.EPSILON)) {
                        for(Symbol t : gRule.LHS.followSet)
                            parseTable[i][SYMBOLS.TERMINALS.indexOf(t)] =
                                    rule.toArray(new Symbol[rule.size()]);
                    }
                    parseTable[i][SYMBOLS.TERMINALS.indexOf(s)] =
                            rule.toArray(new Symbol[rule.size()]);
                }
            }
        }
        printParseTable("Parse_Table.txt");
    }
    
    private void printParseTable(String fileName) {
        int maxLineLength = 0;
        for(Symbol[][] x : parseTable)
            for(Symbol[] y : x) {
                int lineLength = 0;
                if(y != null)
                    for(Symbol z : y)
                        lineLength += z.string.length() + 1;
                lineLength++;
                if(lineLength > maxLineLength)
                    maxLineLength = lineLength;
            }
        try (
            PrintWriter writer = 
                new PrintWriter("outputs/" + fileName, "UTF-8")) {
            for(int i = 0; i < SYMBOLS.maxSymStringLength; i++)
                writer.print(" ");
            writer.print(" |");
            for(Symbol s : SYMBOLS.TERMINALS) {
                writer.print("| ");
                int counter = maxLineLength - s.string.length() - 1;
                for(int i = 0; i < (counter/2); i++)
                    writer.print(" ");
                writer.print(s.string);
                for(int i = 0; i < (counter/2); i++)
                    writer.print(" ");
                if(counter % 2 == 1)
                    writer.print(" ");
                writer.print(" ");
            }
            writer.println();
            for(int i = 0; i < SYMBOLS.maxSymStringLength; i++)
                writer.print("=");
            writer.print("=|");
            for(Symbol s : SYMBOLS.TERMINALS) {
                writer.print("|=");
                for(int i = 0; i < maxLineLength; i++)
                    writer.print("=");
            }
            writer.println();
            for(int i = 0; i < SYMBOLS.NON_TERMINALS.size(); i++) {
                Symbol t = SYMBOLS.NON_TERMINALS.get(i);
                writer.print(t.string);
                for(int j = 0; j < (SYMBOLS.maxSymStringLength - 
                        t.string.length()); j++)
                    writer.print(" ");
                writer.print(" |");
                for(int j = 0; j < SYMBOLS.TERMINALS.size(); j++) {
                    writer.print("| ");
                    if(parseTable[i][j] != null) {
                        int lineLength = 0;
                        for (Symbol s : parseTable[i][j]) {
                            writer.print(s.string);
                            writer.print(" ");
                            lineLength += s.string.length() + 1;
                        }
                        for(int l = 0; l < maxLineLength - lineLength; l++)
                            writer.print(" ");
                    }
                    else
                        for(int l = 0; l < maxLineLength; l++)
                            writer.print(" ");
                }
                writer.println();
            }
        } catch(Exception e) {
            System.out.println(e.toString());
        }
        System.out.println("File created: " + fileName);
    }
    
}
