package parser;

import static parser.Parser.SYMBOLS;

class Token {
    
    Symbol tokenType;
    String token;
    
    Token(String token, String tokenType) {
        this.token = token;
        boolean tokenParse = false;
        for(Symbol s : SYMBOLS)
            if(s.string.equals(tokenType) && s.isTerminal) {
                this.tokenType = s;
                tokenParse = true;
                break;
            }
        if(!tokenParse) {
            System.out.println("\nError: Unable to parse token: " + token +
                    "\t(Token type: " + tokenType + ")\n");
            System.exit(1);
        }
    }
    
    Token(String token, Symbol tokenType) {
        this.token = token;
        this.tokenType = tokenType;
    }
    
}

