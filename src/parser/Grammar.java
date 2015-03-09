package parser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import static parser.Parser.SYMBOLS;

/*

TODO:
    Fix findFFConflict(). It isnt returning a FFC where there is one.

*/

//Reference: http://www.cs.bgu.ac.il/~comp131/wiki.files/ps5.pdf
//Reference: http://research.microsoft.com/pubs/68869/naacl2k-proc-rev.pdf

class Grammar extends ArrayList<GrammarRule>{
    
    int test = 0;

    Grammar(String fileName) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            SYMBOLS.EPSILON = SYMBOLS.add(br.readLine().trim());
            String lineToRead;
            while((lineToRead = br.readLine()) != null)
                addRules(lineToRead);
        } catch(Exception e) {
            System.out.println("Error - reading grammar file");
            System.out.println(e.toString());
            System.exit(1);
        }
        SYMBOLS.printSymbolFile("Symbol_List.txt");
        printGrammarFile("InputGrammar.txt");
    }
    
    Grammar(Grammar g) {
        this.update(g);
    }
    
    Grammar() {
        
    }
    
    @Override
    public boolean add(GrammarRule e) {
        boolean ret;
        for(GrammarRule gr : this)
            if(gr.LHS.equals(e.LHS) && gr.RHS.size() == e.RHS.size()) {
                boolean same = true;
                for(int i = 0; i < gr.RHS.size(); i++)
                    if(!gr.RHS.get(i).equals(e.RHS.get(i))) {
                        same = false;
                        break;
                    }
                if(same)
                    return false;
            }
        if(e.LHS.equals(e.RHS.get(0)) && e.RHS.size() == 1)
            return false;
        ret = super.add(e);
        return ret;
    }
    
    @Override
    public boolean addAll(Collection<? extends GrammarRule> c) {
        boolean ret = false;
        for(GrammarRule o : c) {
            if(!ret)
                ret = this.add(o);
            else
                this.add(o);
        }
        return ret;
    }
    
    public final void printGrammarFile(String fileName) {
        try (
            PrintWriter writer = 
                    new PrintWriter("outputs/" + fileName, "UTF-8")) {
            for(GrammarRule gr : this)
                writer.println(gr.toString());
        } catch(Exception e) {
            System.out.println(e.toString());
        }
        System.out.println("File created: " + fileName);
    }
    
    public final void addRules(String input) {
        String[] split = input.split("->");
        if(split.length != 2)
            return;
        Symbol nonTerminal = SYMBOLS.add(split[0].trim(), false);
        if(SYMBOLS.size() == 2)
            SYMBOLS.START = nonTerminal;
        String[] ruleStrings = split[1].split("\\|");
        for(String rule : ruleStrings) {
            rule = rule.trim();
            if(!rule.equals(""))
                this.add(new GrammarRule(nonTerminal, rule));
        }
    }
    
    public final void update(Grammar g) {
        this.clear();
        for(GrammarRule gr : g)
            this.add(new GrammarRule(gr));
    }

    void makeLL1() {
        removeEpsilonDerivation();
        removeLeftRecursion();
        leftFactor();
        cleanGrammar();
        printGrammarFile("Corrected_Grammar.txt");
    }
    
    List<GrammarRule> getRules(Symbol s) {
        List<GrammarRule> ret = new ArrayList<>();
        for(GrammarRule gr : this)
            if(gr.LHS.equals(s))
                ret.add(gr);
        return ret;
    }
    
    boolean firstAndFollowCheck() {
        FirstFollowIssue ffi = findFFConflict(true);
        if(ffi == null)
            return true;
        System.out.print("\nError: First/Follow conflict with: ");
        System.out.println(ffi.nonTerminal.string);
        System.out.print("Unable to distinguish nonterminals: ");
        for(Symbol t : ffi.issues)
            System.out.print(t.string + " ");
        System.out.println();
        System.out.println("Conflicting rules:");
        for(GrammarRule gr : ffi.rulesWithIssues) {
            System.out.println(gr.toString());
        }
        System.out.println();
        return false;
    }
    
    private FirstFollowIssue findFFConflict(boolean createFiles) {
        SYMBOLS.createFirstSets(this,createFiles);
        SYMBOLS.createFollowSets(this,createFiles);
        List<Symbol> combine;
        for(Symbol s : SYMBOLS.NON_TERMINALS) {
            List<GrammarRule> gRules = this.getRules(s);
            List<List<Symbol>> setsToIntersect = new ArrayList<>();
            for(GrammarRule gr : gRules) {
                List<Symbol> grfs = new ArrayList<>();
                SYMBOLS.firstSetMaker(gr, grfs);
                if(grfs.contains(SYMBOLS.EPSILON)) {
                    grfs.remove(SYMBOLS.EPSILON);
                    grfs.addAll(gr.LHS.followSet);
                }
                setsToIntersect.add(grfs);
            }
            combine = findCommons(setsToIntersect);
            if(combine != null && gRules.size() > 1) {
                FirstFollowIssue ffi = new FirstFollowIssue();
                ffi.nonTerminal = s;
                ffi.issues = combine;
                ffi.rulesWithIssues = new ArrayList<>();
                for(GrammarRule gr : gRules) {
                    List<Symbol> grfs = new ArrayList<>();
                    SYMBOLS.firstSetMaker(gr, grfs);
                    if(grfs.contains(SYMBOLS.EPSILON)) {
                        grfs.remove(SYMBOLS.EPSILON);
                        grfs.addAll(gr.LHS.followSet);
                    }
                    if(!(intersect(combine,grfs)).isEmpty())
                        ffi.rulesWithIssues.add(gr);
                }
                return ffi;
            }
        }
        return null;
    }
    
    @SuppressWarnings("empty-statement")
    private boolean removeEpsilonDerivation() {
        Grammar g = new Grammar(this);
        boolean actionTaken = false;
        boolean loop = true;
        while(loop) {
            loop = false;
            for(GrammarRule epRule : this)
                if(epRule.isEpsilonDerivation()) {
                    boolean delete = false;
                    for(GrammarRule gr : this)
                        if(gr.RHS.contains(epRule.LHS)) {
                            delete = true;
                            GrammarRule newRule = new GrammarRule(gr);
                            while(newRule.RHS.contains(epRule.LHS)) {
                                int index = newRule.RHS.indexOf(epRule.LHS);
                                newRule.RHS.remove(index);
                                newRule.RHS.addAll(index, epRule.RHS);
                            }
                            g.add(newRule);
                        }
                    if(delete) {
                        loop = true;
                        actionTaken = true;
                        g.remove(this.indexOf(epRule));
                        this.update(g);
                        break;
                    }
                }
        }
        return actionTaken;
    }
    
    private boolean removeLeftRecursion() {
        boolean actionTaken = false;
        for(int i = 0; i < SYMBOLS.NON_TERMINALS.size(); i++) {
            Symbol Ai = SYMBOLS.NON_TERMINALS.get(i);
            for(int j = 0; j < i; j++) {
                Symbol Aj = SYMBOLS.NON_TERMINALS.get(j);
                List<GrammarRule> rulesAiAj = new ArrayList<>();
                for(GrammarRule gr : this)
                    if(gr.LHS.equals(Ai) && gr.RHS.get(0).equals(Aj))
                        rulesAiAj.add(gr);
                List<GrammarRule> jRules = this.getRules(Aj);
                List<GrammarRule> rulesToDel = new ArrayList<>();
                List<GrammarRule> rulesToAdd = new ArrayList<>();
                for(GrammarRule ijRule : rulesAiAj) {
                    List<Symbol> ijRHS = new ArrayList<>(ijRule.RHS);
                    ijRHS.remove(0);
                    for(GrammarRule jRule : jRules) {
                        GrammarRule newRule = new GrammarRule(jRule);
                        newRule.LHS = ijRule.LHS;
                        newRule.RHS.addAll(ijRHS);
                        rulesToAdd.add(newRule);
                    }
                    this.addAll(rulesToAdd);
                    this.remove(ijRule);
                    
                    /*  FOR TESTING PURPOSES
                    System.out.println();
                    System.out.println("TEST-DEL-SR");
                    System.out.println(ijRule.toString());
                    System.out.println("TEST-ADD-SR");
                    for(GrammarRule hs : rulesToAdd)
                        System.out.println(hs.toString());
                    System.out.println();
                    */
                    
                }
            }
            boolean recursionExists = false;
            for(GrammarRule gr : this) {
                gr.epsilonCheck();
                if(gr.LHS.equals(Ai) && gr.RHS.get(0).equals(Ai)) {
                    if(gr.RHS.size() == 1) {
                        System.out.print("Error - Cyclic rule: ");
                        System.out.println(gr.RHS.get(0).string);
                        printGrammarFile("Corrected_Grammar.txt");
                        System.exit(1);
                    }
                    recursionExists = true;
                    actionTaken = true;
                    break;
                }
            }
            if(recursionExists) {
                String ntStr = Ai.string + "'";
                while(SYMBOLS.contains(ntStr))
                    ntStr += "'";
                Symbol newSym = SYMBOLS.add(ntStr, false);
                List<GrammarRule> rAdd = new ArrayList<>();
                List<GrammarRule> rDel = new ArrayList<>();
                for(GrammarRule gr : this)
                    if(gr.LHS.equals(Ai)) {
                        List<Symbol> ls = new ArrayList<>(gr.RHS);
                        rDel.add(gr);
                        ls.add(newSym);
                        if(gr.RHS.get(0).equals(Ai)) {
                            ls.remove(0);
                            rAdd.add(new GrammarRule(newSym, ls));
                        }
                        else
                            rAdd.add(new GrammarRule(gr.LHS, ls));
                    }
                rAdd.add(new GrammarRule(newSym));
                this.addAll(rAdd);
                this.removeAll(rDel);
                
                /*  FOR TESTING PURPOSES
                System.out.println();
                System.out.println("TEST-DEL-R");
                for(GrammarRule gr : rDel)
                    System.out.println(gr.toString());
                System.out.println("TEST-ADD-R");
                for(GrammarRule gr : rAdd)
                    System.out.println(gr.toString());
                System.out.println();
                */
                
            }
        }
        return actionTaken;
    }

    private boolean leftFactor() {
        boolean actionTaken = false;
        boolean loop = true;
        
        while(loop) {
            loop = false;
            boolean innerLoop = true;
            FirstFollowIssue ffi = findFFConflict(false);

            while(innerLoop && ffi != null) {
                innerLoop = false;
                for(GrammarRule gr : ffi.rulesWithIssues)
                    for(Symbol s : ffi.issues) {
                        Symbol t = gr.RHS.get(0);
                        GrammarRule x = leftFactorFinder(gr, s);
                        if(x != null)
                            if(!x.RHS.get(0).equals(t)) {
                                actionTaken = true;
                                innerLoop = true;
                                loop = true;
                            }
                    }
                ffi = findFFConflict(false);
            }
            innerLoop = true;
            while(innerLoop) {
                innerLoop = false;
                Symbol nonTerminal = null;
                Symbol symbolMatch = null;
                for (Symbol NON_TERMINAL : SYMBOLS.NON_TERMINALS) {
                    nonTerminal = NON_TERMINAL;
                    symbolMatch = null;
                    List<Symbol> firstSyms = new ArrayList<>();
                    for(GrammarRule gr : this.getRules(nonTerminal)) {
                        if(firstSyms.contains(gr.RHS.get(0)) && 
                                !gr.isEpsilonDerivation()) {
                            loop = true;
                            innerLoop = true;
                            actionTaken = true;
                            symbolMatch = gr.RHS.get(0);
                            break;
                        }
                        else
                            firstSyms.add(gr.RHS.get(0));
                    }
                    if(innerLoop)
                        break;
                }
                if(innerLoop && nonTerminal!= null && symbolMatch != null) {
                    String ntStr = nonTerminal.string + "*";
                    while(SYMBOLS.contains(ntStr))
                        ntStr += "*";
                    Symbol newSym = SYMBOLS.add(ntStr, false);
                    List<GrammarRule> rulesToAdd = new ArrayList<>();
                    List<GrammarRule> rulesToDel = new ArrayList<>();
                    for(GrammarRule gr : this.getRules(nonTerminal))
                        if(gr.RHS.get(0).equals(symbolMatch)) {
                            List<Symbol> newRule = new ArrayList<>(gr.RHS);
                            newRule.remove(0);
                            if(newRule.isEmpty())
                                newRule.add(SYMBOLS.EPSILON);
                            rulesToAdd.add(new GrammarRule(newSym, newRule));
                            rulesToDel.add(gr);  
                        }
                    
                    /*  FOR TESTING PURPOSES
                    System.out.println();
                    System.out.println("TEST-DEL-F");
                    for(GrammarRule gr : rulesToDel)
                        System.out.println(gr.toString());
                    System.out.println("TEST-ADD-F");
                    System.out.println(new GrammarRule(nonTerminal,
                                symbolMatch.string + " " + newSym.string
                                ).toString());
                    for(GrammarRule gr : rulesToAdd)
                        System.out.println(gr.toString());
                    System.out.println();
                    */
                    
                    this.add(new GrammarRule(nonTerminal,
                                symbolMatch.string + " " + newSym.string));
                    this.addAll(rulesToAdd);
                    this.removeAll(rulesToDel);
                }
            }
        }
        return actionTaken;
    }
    
    private GrammarRule leftFactorFinder(GrammarRule gr, Symbol s) {
        if(gr.RHS.get(0).equals(s))
            return gr;
        else if(gr.RHS.get(0).isTerminal)
            return null;
        else {
            for(GrammarRule rule : this.getRules(gr.RHS.get(0))) {
                GrammarRule sub = leftFactorFinder(rule, s);
                if(sub != null) {
                    List<GrammarRule> oldRules = new ArrayList<>();
                        //NOTE: The list "oldRules" is used for testing
                    List<GrammarRule> newRules = new ArrayList<>();
                    for(GrammarRule hs : this)
                        if(hs.RHS.contains(sub.LHS)) {
                            oldRules.add(hs);
                            GrammarRule newRule = new GrammarRule(hs);
                            while(newRule.RHS.contains(sub.LHS)) {
                                int i = newRule.RHS.indexOf(sub.LHS);
                                newRule.RHS.remove(i);
                                newRule.RHS.addAll(i, sub.RHS);
                            }
                            newRules.add(newRule);
                        }
                    
                    /*  FOR TESTING PURPOSES
                    System.out.println();
                    System.out.println("TEST-SF-SUB");
                    System.out.println(sub.toString());
                    System.out.println("TEST-OLD-SF");
                    for(GrammarRule hs : oldRules)
                        System.out.println(hs.toString());
                    System.out.println("TEST-ADD-SF");
                    for(GrammarRule hs : newRules)
                        System.out.println(hs.toString());
                    System.out.println();
                    */
                    
                    this.remove(sub);
                    for(GrammarRule hs : newRules)
                        this.add(hs);
                }
            }
            return gr;
        }
    }
    
    private boolean cleanGrammar() {
        boolean actionTaken = false;
        boolean loop = true;
        while(loop) {
            loop = false;
            List<Symbol> symsToDel = new ArrayList<>();
            for(Symbol s : SYMBOLS) {
                boolean rhsContainsS = false;
                for(GrammarRule gr : this)
                    if(gr.RHS.contains(s)) {
                        rhsContainsS = true;
                        break;
                    }
                if(!rhsContainsS && !s.equals(SYMBOLS.START)) {
                    actionTaken = true;
                    loop = true;
                    if(!s.isTerminal)
                        this.removeAll(this.getRules(s));
                    symsToDel.add(s);
                }
                else if(!s.isTerminal && !s.equals(SYMBOLS.START)){
                    boolean ruleExists = false;
                    for(GrammarRule gr : this)
                        if(gr.LHS.equals(s)) {
                            ruleExists = true;
                            break;
                        }
                    if(!ruleExists) {
                        actionTaken = true;
                        loop = true;
                        List<GrammarRule> rulesToDel = new ArrayList<>();
                        for(GrammarRule gr : this)
                            if(gr.RHS.contains(s))
                                rulesToDel.add(gr);
                        this.removeAll(rulesToDel);
                    }
                }
            }
            SYMBOLS.removeAll(symsToDel);
        }
        return actionTaken;
    }
    
    private <T> ArrayList<T> intersect(List<T> L1, List<T> L2) {
        ArrayList<T> list = new ArrayList<>();
        for (T t : L1)
            if(L2.contains(t))
                list.add(t);
        return list;
    }
    
    private List<Symbol> findCommons(List<List<Symbol>> list) {
        List<Symbol> commons;
        for(int i = 0; i < list.size(); i++)
            for(int j = 0; j < i; j++) {
                commons = intersect(list.get(i),list.get(j));
                if(!commons.isEmpty())
                    return commons;
            }
        return null;
    }
    
    private class FirstFollowIssue {
        Symbol nonTerminal;
        List<Symbol> issues;
        List<GrammarRule> rulesWithIssues;
    }
    
}

class GrammarRule {
    
    Symbol LHS;
    List<Symbol> RHS = new ArrayList<>();
    
    GrammarRule(Symbol LHS, List<Symbol> RHS) {
        this.LHS = LHS;
        this.RHS = RHS;
        epsilonCheck();
    }
    
    GrammarRule(Symbol LHS, String RHS) {
        this.LHS = LHS;
        createRHS(RHS);
    }
    
    GrammarRule(String rule) {
        String[] ruleSplit = rule.split("->");
        this.LHS = SYMBOLS.add(ruleSplit[0].trim(), false);
        createRHS(ruleSplit[1].trim());
    }
    
    GrammarRule(Symbol LHS) {
        this.LHS = LHS;
        RHS.add(SYMBOLS.EPSILON);
    }
    
    GrammarRule(GrammarRule gr) {
        this.LHS = gr.LHS;
        for(Symbol s : gr.RHS)
            this.RHS.add(s);
        epsilonCheck();
    }
    
    @Override
    public String toString() {
        String str = LHS.string + " ->";
        for(Symbol s : RHS)
            str += " " + s.string;
        return str;
    }
    
    public boolean isEpsilonDerivation() {
        return RHS.size() == 1 && RHS.get(0).equals(SYMBOLS.EPSILON);
    }
    
    public boolean RHSisIdentical() {
        boolean ret = true;
        Symbol test = RHS.get(0);
        for(Symbol s : RHS)
            if(!s.equals(test))
                ret = false;
        return ret;
            
    }
    
    public final void epsilonCheck() {
        while(this.RHS.size() != 1 && this.RHS.contains(SYMBOLS.EPSILON))
            this.RHS.remove(SYMBOLS.EPSILON);
        if(this.RHS.isEmpty())
            this.RHS.add(SYMBOLS.EPSILON);
    }
    
    private void createRHS(String RHS) {
        for(String ruleSym : RHS.split(" ")) {
            ruleSym = ruleSym.trim();
            if(!ruleSym.equals("")) {
                this.RHS.add(SYMBOLS.add(ruleSym));
            }
        }
        epsilonCheck();
    }
    
}