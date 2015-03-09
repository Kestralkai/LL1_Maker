package parser;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

class SymbolList extends ArrayList<Symbol>{
    
    Symbol EPSILON;
    Symbol START;
    Symbol TERMINATE = new Symbol("$");
    List<Symbol> NON_TERMINALS = new ArrayList<>();
    List<Symbol> TERMINALS = new ArrayList<>();
    int maxSymStringLength = 0;
    
    SymbolList() {
        TERMINALS.add(TERMINATE);
    }

    public Symbol add(String symbolName) {
        int index = this.indexOf(symbolName);
        if(index != -1)
            return this.get(index);
        Symbol s = new Symbol(symbolName);
        if(s.string.length() > maxSymStringLength)
            maxSymStringLength = s.string.length();
        this.add(s);
        TERMINALS.add(s);
        return s;
    }
    
    public Symbol add(String symbolName, boolean isTerminal) {
        int index = this.indexOf(symbolName);
        if(index != -1) {
            if(this.get(index).isTerminal && !isTerminal) {
                TERMINALS.remove(this.get(index));
                NON_TERMINALS.add(this.get(index));
            } else if(!this.get(index).isTerminal && isTerminal) {
                TERMINALS.add(this.get(index));
                NON_TERMINALS.remove(this.get(index));
            }
            this.get(index).isTerminal = isTerminal;
            return this.get(index);
        }
        Symbol s = new Symbol(symbolName, isTerminal);
        if(s.string.length() > maxSymStringLength)
            maxSymStringLength = s.string.length();
        this.add(s);
        if(isTerminal)
            TERMINALS.add(s);
        else
            NON_TERMINALS.add(s);
        return s;
    }
    
    public boolean remove(Symbol s) {
        boolean ret = super.remove(s);
        if(ret) {
            this.NON_TERMINALS.remove(s);
            this.TERMINALS.remove(s);
        }
        return ret;
    }
    
    public boolean removeAll(List<Symbol> ls) {
        boolean ret = false;
        for(Symbol s : ls)
            if(!ret)
                ret = super.remove(s);
            else
                super.remove(s);
        return ret;
    }
    
    public boolean contains(String symbolName) {
        boolean ret = false;
        for(Symbol s : this)
            if(s.string.equals(symbolName))
                ret = true;
        return ret;
    }
    
    public int indexOf(String symbolName) {
        for(int i = 0; i < this.size(); i++)
            if(this.get(i).string.equals(symbolName))
                return i;
        return -1;
    }
    
    public int firstSetMaker(GrammarRule gr, List<Symbol> fs) {
        int numChanges = 0;
        for(int i = 0; i < gr.RHS.size(); i++) {
            Symbol s = gr.RHS.get(i);
            if(s.isTerminal && !fs.contains(s)) {
                fs.add(s);
                numChanges++;
                break;
            } else if (!s.isTerminal) {
                int combine = 
                        combineFirstSets(fs, s.firstSet);
                if(combine == -1 && i == gr.RHS.size()-1 && 
                        !fs.contains(EPSILON)) {
                    fs.add(EPSILON);
                    numChanges++;
                } else if(combine >= 0) {
                    numChanges += combine;
                    break;
                }
            } else
                break;
        }
        return numChanges;
    }

    void createFirstSets(Grammar g, boolean createFile) {
        for(Symbol s : this)
            s.firstSet = new ArrayList<>();
        int numChanges;
        do {
            numChanges = 0;
            for(GrammarRule gr : g) {
                numChanges += firstSetMaker(gr,gr.LHS.firstSet);
            }
        } while(numChanges > 0);
        for(Symbol s : this)
            if(s.isTerminal)
                s.firstSet.add(s);
        if(createFile) {
            try (
                PrintWriter writer = 
                        new PrintWriter("outputs/First_Sets.txt", "UTF-8")) {
                for(Symbol s : NON_TERMINALS) {
                    writer.print(s.string);
                    for(int i = s.string.length(); i < maxSymStringLength; i++)
                        writer.print(" ");
                    writer.print("\t");
                    for (Symbol firstSet : s.firstSet) {
                        writer.print(firstSet.string + " ");
                    }
                    writer.println();
                }
            } catch(Exception e) {
                System.out.println(e.toString());
            }
            System.out.println("File created: First_Sets.txt");
        }
    }

    void createFollowSets(Grammar g, boolean createFile) {
        for(Symbol s : this)
            s.followSet = new ArrayList<>();
        int numChanges;
        START.followSet.add(TERMINATE);
        do {
            numChanges = 0;
            for(GrammarRule gr : g)
                for(int i = 0; i < gr.RHS.size(); i++) {
                    Symbol s = gr.RHS.get(i);
                    if(!s.isTerminal) {
                        if(i == gr.RHS.size()-1)
                            numChanges += combineFollowSets(s.followSet, 
                                    gr.LHS.followSet);
                        else if(gr.RHS.get(i+1).isTerminal) {
                            if(!s.followSet.contains(gr.RHS.get(i+1))) {
                                s.followSet.add(gr.RHS.get(i+1));
                                numChanges++;
                            }
                        } else {
                            int x;
                            for(x = i+1; x < gr.RHS.size(); x++) {
                                Symbol t = gr.RHS.get(x);
                                int combine = addFirstToFollow(s, t);
                                if(combine % 2 == 1)
                                    numChanges++;
                                if(combine < 2)
                                    break;
                            }
                            if(x == gr.RHS.size())
                                numChanges += combineFollowSets(
                                        s.followSet, gr.LHS.followSet);
                        }
                    }
                }
        } while(numChanges > 0);
        if(createFile) {
            try (
                PrintWriter writer = 
                        new PrintWriter("outputs/Follow_Sets.txt", "UTF-8")) {
                for(Symbol s : NON_TERMINALS) {
                    writer.print(s.string);
                    for(int i = s.string.length(); i < maxSymStringLength; i++)
                        writer.print(" ");
                    writer.print("\t");
                    for (Symbol followSet : s.followSet) {
                        writer.print(followSet.string + " ");
                    }
                    writer.println();
                }
            } catch(Exception e) {
                System.out.println(e.toString());
            }
            System.out.println("File created: Follow_Sets.txt");
        }
    }
    
    private int combineFirstSets(List<Symbol> S1, List<Symbol> S2) {
        int ret = 0;
        boolean epsilon = false;
        for(Symbol s : S2) {
            if(s.equals(EPSILON))
                epsilon = true;
            else if(!S1.contains(s)) {
                ret = 1;
                S1.add(s);
            }
        }
        return epsilon? -1 : ret;
    }
    
    private int combineFollowSets(List<Symbol> S1, List<Symbol> S2) {
        int ret = 0;
        for(Symbol s : S2) {
            if(!S1.contains(s)) {
                ret = 1;
                S1.add(s);
            }
        }
        return ret;
    }

    private int addFirstToFollow(Symbol S1, Symbol S2) {
        /*
            This method adds the first set of S2 to  the first set of S1, with
            return codes as follows:
            0: No change was made
            1: A change took place, and epsilon is not in the first set of S2
            2: No change took place, but the first set of S2 contains epsilon
            3: A change took place, and epsilon is in the first set of S2
        */
        if(S2.isTerminal) {
            if(!S1.followSet.contains(S2)) {
                S1.followSet.add(S2);
                return 1;
            }
            else
                return 0;
        }
        int changes = 0;
        boolean add = false;
        for(Symbol s : S2.firstSet) {
            if(s.equals(EPSILON))
                changes += 2;
            else if(!S1.followSet.contains(s)) {
                add = true;
                S1.followSet.add(s);
            }
        }
        if(add)
            changes += 1;
        return changes;
    }

    void printSymbolFile(String fileName) {
        try (
            PrintWriter writer = 
                    new PrintWriter("outputs/" + fileName, "UTF-8")) {
            String str = "Symbol";
            for(int i=0; i < (maxSymStringLength-6); i++)
                str += " ";
            writer.println(str + "\tTerminal");
            for(Symbol sym : this) {
                str = "";
                for(int i=0; i < (maxSymStringLength-sym.string.length()); i++)
                    str += " ";
                str += "\t";
                writer.println(sym.string + str + sym.isTerminal);
            }
        } catch(Exception e) {
            System.out.println(e.toString());
        }
        System.out.println("File created: " + fileName);
    }
    
}

class Symbol {
    
    String string;
    boolean isTerminal;
    List<Symbol> firstSet = new ArrayList<>();
    List<Symbol> followSet = new ArrayList<>();

    Symbol(String string) {
        this.string = string;
        isTerminal = true;
    }
    
    Symbol(String string, boolean isTerminal) {
        this.string = string;
        this.isTerminal = isTerminal;
    }
    
}
