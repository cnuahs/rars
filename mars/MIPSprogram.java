package mars;

import mars.assembler.*;
import mars.mips.hardware.RegisterFile;
import mars.simulator.BackStepper;
import mars.simulator.Simulator;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

/*
Copyright (c) 2003-2006,  Pete Sanderson and Kenneth Vollmar

Developed by Pete Sanderson (psanderson@otterbein.edu)
and Kenneth Vollmar (kenvollmar@missouristate.edu)

Permission is hereby granted, free of charge, to any person obtaining 
a copy of this software and associated documentation files (the 
"Software"), to deal in the Software without restriction, including 
without limitation the rights to use, copy, modify, merge, publish, 
distribute, sublicense, and/or sell copies of the Software, and to 
permit persons to whom the Software is furnished to do so, subject 
to the following conditions:

The above copyright notice and this permission notice shall be 
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

(MIT license, http://www.opensource.org/licenses/mit-license.html)
 */

/**
 * Internal representations of MIPS program.  Connects source, tokens and machine code.  Having
 * all these structures available facilitates construction of good messages,
 * debugging, and easy simulation.
 *
 * @author Pete Sanderson
 * @version August 2003
 **/

public class MIPSprogram {

    // See explanation of method inSteppedExecution() below.
    private boolean steppedExecution = false;

    private String filename;
    private ArrayList<String> sourceList;
    private ArrayList<TokenList> tokenList;
    private ArrayList<ProgramStatement> parsedList;
    private ArrayList<ProgramStatement> machineList;
    private BackStepper backStepper;
    private SymbolTable localSymbolTable;
    private MacroPool macroPool;
    private ArrayList<SourceLine> sourceLineList;
    private Tokenizer tokenizer;

    /**
     * Produces list of source statements that comprise the program.
     *
     * @return ArrayList of String.  Each String is one line of MIPS source code.
     **/

    public ArrayList<String> getSourceList() {
        return sourceList;
    }

    /**
     * Set list of source statements that comprise the program.
     *
     * @param sourceLineList ArrayList of SourceLine.
     *                       Each SourceLine represents one line of MIPS source code.
     **/

    public void setSourceLineList(ArrayList<SourceLine> sourceLineList) {
        this.sourceLineList = sourceLineList;
        sourceList = new ArrayList<>();
        for (SourceLine sl : sourceLineList) {
            sourceList.add(sl.getSource());
        }
    }

    /**
     * Retrieve list of source statements that comprise the program.
     *
     * @return ArrayList of SourceLine.
     * Each SourceLine represents one line of MIPS source cod
     **/

    public ArrayList<SourceLine> getSourceLineList() {
        return this.sourceLineList;
    }

    /**
     * Produces name of associated source code file.
     *
     * @return File name as String.
     **/

    public String getFilename() {
        return filename;
    }

    /**
     * Produces list of tokens that comprise the program.
     *
     * @return ArrayList of TokenList.  Each TokenList is list of tokens generated by
     * corresponding line of MIPS source code.
     * @see TokenList
     **/

    public ArrayList<TokenList> getTokenList() {
        return tokenList;
    }

    /**
     * Retrieves Tokenizer for this program
     *
     * @return Tokenizer
     **/

    public Tokenizer getTokenizer() {
        return tokenizer;
    }

    /**
     * Produces new empty list to hold parsed source code statements.
     *
     * @return ArrayList of ProgramStatement.  Each ProgramStatement represents a parsed
     * MIPS statement.
     * @see ProgramStatement
     **/

    public ArrayList<ProgramStatement> createParsedList() {
        parsedList = new ArrayList<>();
        return parsedList;
    }

    /**
     * Produces existing list of parsed source code statements.
     *
     * @return ArrayList of ProgramStatement.  Each ProgramStatement represents a parsed
     * MIPS statement.
     * @see ProgramStatement
     **/

    public ArrayList<ProgramStatement> getParsedList() {
        return parsedList;
    }

    /**
     * Produces list of machine statements that are assembled from the program.
     *
     * @return ArrayList of ProgramStatement.  Each ProgramStatement represents an assembled
     * basic MIPS instruction.
     * @see ProgramStatement
     **/

    public ArrayList<ProgramStatement> getMachineList() {
        return machineList;
    }


    /**
     * Returns BackStepper associated with this program.  It is created upon successful assembly.
     *
     * @return BackStepper object, null if there is none.
     **/

    public BackStepper getBackStepper() {
        return backStepper;
    }

    /**
     * Returns SymbolTable associated with this program.  It is created at assembly time,
     * and stores local labels (those not declared using .globl directive).
     **/

    public SymbolTable getLocalSymbolTable() {
        return localSymbolTable;
    }

    /**
     * Returns status of BackStepper associated with this program.
     *
     * @return true if enabled, false if disabled or non-existant.
     **/

    public boolean backSteppingEnabled() {
        return (backStepper != null && backStepper.enabled());
    }

    /**
     * Produces specified line of MIPS source program.
     *
     * @param i Line number of MIPS source program to get.  Line 1 is first line.
     * @return Returns specified line of MIPS source.  If outside the line range,
     * it returns null.  Line 1 is first line.
     **/

    public String getSourceLine(int i) {
        if ((i >= 1) && (i <= sourceList.size()))
            return sourceList.get(i - 1);
        else
            return null;
    }


    /**
     * Reads MIPS source code from file into structure.  Will always read from file.
     * It is GUI responsibility to assure that source edits are written to file
     * when user selects compile or run/step options.
     *
     * @param file String containing name of MIPS source code file.
     * @throws AssemblyException Will throw exception if there is any problem reading the file.
     **/

    public void readSource(String file) throws AssemblyException {
        this.filename = file;
        this.sourceList = new ArrayList<>();
        ErrorList errors;
        BufferedReader inputFile;
        String line;
        try {
            inputFile = new BufferedReader(new FileReader(file));
            line = inputFile.readLine();
            while (line != null) {
                sourceList.add(line);
                line = inputFile.readLine();
            }
        } catch (Exception e) {
            errors = new ErrorList();
            errors.add(new ErrorMessage((MIPSprogram) null, 0, 0, e.toString()));
            throw new AssemblyException(errors);
        }
    }

    /**
     * Tokenizes the MIPS source program. Program must have already been read from file.
     *
     * @throws AssemblyException Will throw exception if errors occured while tokenizing.
     **/

    public void tokenize() throws AssemblyException {
        this.tokenizer = new Tokenizer();
        this.tokenList = tokenizer.tokenize(this);
        this.localSymbolTable = new SymbolTable(this.filename); // prepare for assembly
    }

    /**
     * Prepares the given list of files for assembly.  This involves
     * reading and tokenizing all the source files.  There may be only one.
     *
     * @param filenames        ArrayList containing the source file name(s) in no particular order
     * @param leadFilename     String containing name of source file that needs to go first and
     *                         will be represented by "this" MIPSprogram object.
     * @param exceptionHandler String containing name of source file containing exception
     *                         handler.  This will be assembled first, even ahead of leadFilename, to allow it to
     *                         include "startup" instructions loaded beginning at 0x00400000.  Specify null or
     *                         empty String to indicate there is no such designated exception handler.
     * @return ArrayList containing one MIPSprogram object for each file to assemble.
     * objects for any additional files (send ArrayList to assembler)
     * @throws AssemblyException Will throw exception if errors occured while reading or tokenizing.
     **/

    public ArrayList<MIPSprogram> prepareFilesForAssembly(ArrayList<String> filenames, String leadFilename, String exceptionHandler) throws AssemblyException {
        ArrayList<MIPSprogram> MIPSprogramsToAssemble = new ArrayList<>();
        int leadFilePosition = 0;
        if (exceptionHandler != null && exceptionHandler.length() > 0) {
            filenames.add(0, exceptionHandler);
            leadFilePosition = 1;
        }
        for (String filename : filenames) {
            MIPSprogram preparee = (filename.equals(leadFilename)) ? this : new MIPSprogram();
            preparee.readSource(filename);
            preparee.tokenize();
            // I want "this" MIPSprogram to be the first in the list...except for exception handler
            if (preparee == this && MIPSprogramsToAssemble.size() > 0) {
                MIPSprogramsToAssemble.add(leadFilePosition, preparee);
            } else {
                MIPSprogramsToAssemble.add(preparee);
            }
        }
        return MIPSprogramsToAssemble;
    }

    /**
     * Assembles the MIPS source program. All files comprising the program must have
     * already been tokenized.  Assembler warnings are not considered errors.
     *
     * @param MIPSprogramsToAssemble   ArrayList of MIPSprogram objects, each representing a tokenized source file.
     * @param extendedAssemblerEnabled A boolean value - true means extended (pseudo) instructions
     *                                 are permitted in source code and false means they are to be flagged as errors.
     * @return ErrorList containing nothing or only warnings (otherwise would have thrown exception).
     * @throws AssemblyException Will throw exception if errors occured while assembling.
     **/

    public ErrorList assemble(ArrayList<MIPSprogram> MIPSprogramsToAssemble, boolean extendedAssemblerEnabled)
            throws AssemblyException {
        return assemble(MIPSprogramsToAssemble, extendedAssemblerEnabled, false);
    }

    /**
     * Assembles the MIPS source program. All files comprising the program must have
     * already been tokenized.
     *
     * @param MIPSprogramsToAssemble   ArrayList of MIPSprogram objects, each representing a tokenized source file.
     * @param extendedAssemblerEnabled A boolean value - true means extended (pseudo) instructions
     *                                 are permitted in source code and false means they are to be flagged as errors
     * @param warningsAreErrors        A boolean value - true means assembler warnings will be considered errors and terminate
     *                                 the assemble; false means the assembler will produce warning message but otherwise ignore warnings.
     * @return ErrorList containing nothing or only warnings (otherwise would have thrown exception).
     * @throws AssemblyException Will throw exception if errors occured while assembling.
     **/

    public ErrorList assemble(ArrayList<MIPSprogram> MIPSprogramsToAssemble, boolean extendedAssemblerEnabled,
                              boolean warningsAreErrors) throws AssemblyException {
        this.backStepper = null;
        Assembler asm = new Assembler();
        this.machineList = asm.assemble(MIPSprogramsToAssemble, extendedAssemblerEnabled, warningsAreErrors);
        this.backStepper = new BackStepper();
        return asm.getErrorList();
    }



    /**
     * Simulates execution of the MIPS program (in this thread). Program must have already been assembled.
     * Begins simulation at current program counter address and continues until stopped,
     * paused, maximum steps exceeded, or exception occurs.
     *
     * @param maxSteps the maximum maximum number of steps to simulate.
     * @return true if execution completed and false otherwise
     * @throws SimulationException Will throw exception if errors occured while simulating.
     */
    public boolean simulate(int maxSteps) throws SimulationException {
        steppedExecution = false;
        Simulator sim = Simulator.getInstance();
        return sim.simulate(RegisterFile.getProgramCounter(), maxSteps, null);
    }

    /**
     * Simulates execution of the MIPS program (in a new thread). Program must have already been assembled.
     * Begins simulation at current program counter address and continues until stopped,
     * paused, maximum steps exceeded, or exception occurs.
     *
     * @param breakPoints int array of breakpoints (PC addresses).  Can be null.
     * @param maxSteps    maximum number of instruction executions.  Default -1 means no maximum.
     * @param a           the GUI component responsible for this call (GO normally).  set to null if none.
     **/
    public void startSimulation(int[] breakPoints, int maxSteps, AbstractAction a) {
        steppedExecution = false;
        Simulator sim = Simulator.getInstance();
        sim.startSimulation(RegisterFile.getProgramCounter(), maxSteps, breakPoints, a);
    }

    /**
     * Will be true only while in process of simulating a program statement
     * in step mode (e.g. returning to GUI after each step).  This is used to
     * prevent spurious AccessNotices from being sent from Memory and Register
     * to observers at other times (e.g. while updating the data and register
     * displays, while assembling program's data segment, etc).
     */
    public boolean inSteppedExecution() {
        return steppedExecution;
    }

    /**
     * Instantiates a new {@link MacroPool} and sends reference of this
     * {@link MIPSprogram} to it
     *
     * @return instatiated MacroPool
     * @author M.H.Sekhavat <sekhavat17@gmail.com>
     */
    public MacroPool createMacroPool() {
        macroPool = new MacroPool(this);
        return macroPool;
    }

    /**
     * Gets local macro pool {@link MacroPool} for this program
     *
     * @return MacroPool
     * @author M.H.Sekhavat <sekhavat17@gmail.com>
     */
    public MacroPool getLocalMacroPool() {
        return macroPool;
    }

    /**
     * Sets local macro pool {@link MacroPool} for this program
     *
     * @param macroPool reference to MacroPool
     * @author M.H.Sekhavat <sekhavat17@gmail.com>
     */
    public void setLocalMacroPool(MacroPool macroPool) {
        this.macroPool = macroPool;
    }

}  // MIPSprogram
