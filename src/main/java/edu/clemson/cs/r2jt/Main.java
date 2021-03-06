/**
 * Main.java
 * ---------------------------------
 * Copyright (c) 2014
 * RESOLVE Software Research Group
 * School of Computing
 * Clemson University
 * All rights reserved.
 * ---------------------------------
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */
package edu.clemson.cs.r2jt;

import java.io.File;
import java.util.HashMap;

import edu.clemson.cs.r2jt.absyn.ModuleDec;
import edu.clemson.cs.r2jt.archiving.Archiver;
import edu.clemson.cs.r2jt.collections.Iterator;
import edu.clemson.cs.r2jt.collections.List;
import edu.clemson.cs.r2jt.compilereport.CompileReport;
import edu.clemson.cs.r2jt.congruenceclassprover.CongruenceClassProver;
import edu.clemson.cs.r2jt.congruenceclassprover.SMTProver;
import edu.clemson.cs.r2jt.data.MetaFile;
import edu.clemson.cs.r2jt.data.ModuleKind;
import edu.clemson.cs.r2jt.errors.ErrorHandler;
import edu.clemson.cs.r2jt.init.CompileEnvironment;
import edu.clemson.cs.r2jt.init.Controller;
import edu.clemson.cs.r2jt.translation.*;
import edu.clemson.cs.r2jt.typeandpopulate.MathSymbolTableBuilder;
import edu.clemson.cs.r2jt.rewriteprover.Prover;
import edu.clemson.cs.r2jt.rewriteprover.AlgebraicProver;
import edu.clemson.cs.r2jt.rewriteprover.ProverListener;
import edu.clemson.cs.r2jt.misc.Flag;
import edu.clemson.cs.r2jt.misc.FlagDependencies;
import edu.clemson.cs.r2jt.misc.FlagDependencyException;
import edu.clemson.cs.r2jt.vcgeneration.VCGenerator;

/**
 * The main class for the Resolve compiler.
 */
public class Main {

    private static final String VERSION = "Fall 2014";

    public static final String FLAG_SECTION_GENERAL = "General";

    public static final Flag FLAG_HELP =
            new Flag(FLAG_SECTION_GENERAL, "help",
                    "Displays this help information.");

    public static final Flag FLAG_EXTENDED_HELP =
            new Flag("General", "xhelp",
                    "Displays all flags, including development flags and many others "
                            + "not relevant to most users.");

    public static final Flag FLAG_NO_STANDARD_IMPORT =
            new Flag("General", "nostdimport",
                    "Prevents the compiler from importing standard uses modules.");

    //private static boolean      bodies      = false;
    private static boolean compileDirs = false;
    private static String mainDirName = "Main";

    //private static Environment env;

    public static void main(String[] args) {
        //Environment.newInstance();
        //env = Environment.getInstance();

        setUpFlagDependencies();

        try {
            CompileEnvironment compileEnvironment =
                    new CompileEnvironment(args);
            args = compileEnvironment.getRemainingArgs();
            ErrorHandler err = new ErrorHandler(compileEnvironment);
            compileEnvironment.setErrorHandler(err);
            //compileEnvironment.setUserFileMap(getFakeHashMap());
            //Environment env = new Environment(compileEnvironment);
            //env.setErrorHandler(err);
            String preferredMainDirectory = null;

            List<File> files = new List<File>();
            if (args.length >= 1
                    && !compileEnvironment.flags.isFlagSet(FLAG_HELP)) {

                for (int i = 0; i < args.length; i++) {
                    if (args[i].equals("-showBuild")) {
                        compileEnvironment.setShowBuildFlag();
                    }
                    else if (args[i].equals("-showEnv")) {
                        compileEnvironment.setShowEnvFlag();
                    }
                    else if (args[i].equals("-showTable")) {
                        compileEnvironment.setShowTableFlag();
                    }
                    else if (args[i].equals("-showBind")) {
                        compileEnvironment.setShowBindFlag();
                    }
                    else if (args[i].equals("-showImports")) {
                        compileEnvironment.setShowImportsFlag();
                    }
                    else if (args[i].equals("-showIndirect")) {
                        compileEnvironment.setShowIndirectFlag();
                    }
                    else if (args[i].equals("-R")) {
                        compileDirs = true;
                    }
                    else if (args[i].equals("-PVCs")) {
                        compileEnvironment.setPerformanceFlag();
                    }
                    else if (args[i].equalsIgnoreCase("-maindir")) {
                        if (i + 1 < args.length) {
                            i++;
                            preferredMainDirectory = args[i];
                        }
                    }
                    else if (args[i].equals("-D")) {
                        if (i + 1 < args.length) {
                            i++;
                            mainDirName = args[i];
                        }
                    }
                    else if (args[i].equals("-o")) {
                        if (i + 1 < args.length) {
                            String outputFile;
                            i++;
                            outputFile = args[i];
                            compileEnvironment.setOutputFileName(outputFile);
                        }
                    }
                    else {
                        files.add(getAbsoluteFile(args[i]));
                    }
                }

                if (!compileEnvironment.flags
                        .isFlagSet(ResolveCompiler.FLAG_WEB)) {
                    System.out.println("RESOLVE Compiler/Verifier - " + VERSION
                            + " Version.");
                    System.out.println("  Use -help flag for options.");
                }
                if (compileEnvironment.flags
                        .isFlagSet(ResolveCompiler.FLAG_NO_DEBUG)) {
                    compileEnvironment.setDebugOff();
                }

                setupEnv(preferredMainDirectory, compileEnvironment);
                MetaFile dummy = null;
                compileFiles(files, compileEnvironment, dummy);
            }
            else {
                printHelpMessage(compileEnvironment);
            }
        }
        catch (FlagDependencyException fde) {
            System.out.println("RESOLVE Compiler/Verifier - " + VERSION
                    + " Version.");
            System.out.println("  Use -help flag for options.");
            System.err.println(fde.getMessage());
        }
    }

    public static void runMain(String[] args, CompileReport rep,
            MetaFile inputFile, HashMap<String, MetaFile> userFileMap) {
        runMain(args, rep, inputFile, userFileMap, null);
    }

    /*
     * Added this so we could make the CompileReport non static, and pass
     * it in from the ResolveCompiler class (for use in the web interface)
     */
    public static void runMain(String[] args, CompileReport rep,
            MetaFile inputFile, HashMap<String, MetaFile> userFileMap,
            ProverListener listener) {
        //Environment.newInstance();
        //env = Environment.getInstance();

        setUpFlagDependencies();
        String fileName = inputFile.getMyFileName();
        String fileSource = inputFile.getMyFileSource();
        try {
            CompileEnvironment compileEnvironment =
                    new CompileEnvironment(args);
            compileEnvironment.setCompileReport(rep);
            compileEnvironment.setTargetFileName(fileName);
            compileEnvironment.setTargetSource(fileSource);
            compileEnvironment.setUserFileMap(userFileMap);
            compileEnvironment.setProverListener(listener);
            args = compileEnvironment.getRemainingArgs();
            ErrorHandler err = new ErrorHandler(compileEnvironment);
            compileEnvironment.setErrorHandler(err);
            //Environment env = new Environment(compileEnvironment);
            //env.setErrorHandler(err);

            String preferredMainDirectory = null;

            List<File> files = new List<File>();
            if (args.length >= 1
                    && !compileEnvironment.flags.isFlagSet(FLAG_HELP)) {

                for (int i = 0; i < args.length; i++) {
                    if (args[i].equals("-showBuild")) {
                        compileEnvironment.setShowBuildFlag();
                    }
                    else if (args[i].equals("-showEnv")) {
                        compileEnvironment.setShowEnvFlag();
                    }
                    else if (args[i].equals("-showTable")) {
                        compileEnvironment.setShowTableFlag();
                    }
                    else if (args[i].equals("-showBind")) {
                        compileEnvironment.setShowBindFlag();
                    }
                    else if (args[i].equals("-showImports")) {
                        compileEnvironment.setShowImportsFlag();
                    }
                    else if (args[i].equals("-showIndirect")) {
                        compileEnvironment.setShowIndirectFlag();
                    }
                    else if (args[i].equals("-R")) {
                        compileDirs = true;
                    }
                    else if (args[i].equalsIgnoreCase("-maindir")) {
                        if (i + 1 < args.length) {
                            i++;
                            preferredMainDirectory = args[i];
                        }
                    }
                    else if (args[i].equals("-D")) {
                        if (i + 1 < args.length) {
                            i++;
                            mainDirName = args[i];
                        }
                    }
                    else if (args[i].equals("-o")) {
                        if (i + 1 < args.length) {
                            String outputFile;
                            i++;
                            outputFile = args[i];
                            compileEnvironment.setOutputFileName(outputFile);
                        }
                    }
                    else {
                        files.add(getAbsoluteFile(args[i]));
                    }
                }

                if (!compileEnvironment.flags
                        .isFlagSet(ResolveCompiler.FLAG_WEB)) {
                    System.out.println("RESOLVE Compiler/Verifier - " + VERSION
                            + " Version.");
                    System.out.println("  Use -help flag for options.");
                }
                if (compileEnvironment.flags
                        .isFlagSet(ResolveCompiler.FLAG_NO_DEBUG)) {
                    compileEnvironment.setDebugOff();
                }

                setupEnv(preferredMainDirectory, compileEnvironment);
                compileFiles(files, compileEnvironment, inputFile);
            }
            else {
                printHelpMessage(compileEnvironment);
            }
        }
        catch (FlagDependencyException fde) {
            System.out.println("RESOLVE Compiler/Verifier - " + VERSION
                    + " Version.");
            System.out.println("  Use -help flag for options.");
            System.err.println(fde.getMessage());
        }
    }

    /**
     * Sets up the compilation environment
     */
    private static void setupEnv(String preferredMainDirectory,
            CompileEnvironment env) {
        /*if (bodies) {
            env.setCompileBodiesFlag();
        }*/
        env.setMainDir(getMainDir(preferredMainDirectory));
    }

    /**
     * Iterates over the files in the list and compiles them one
     * at a time.
     */
    private static void compileFiles(List<File> files,
            CompileEnvironment instanceEnvironment, MetaFile inputFile) {

        MathSymbolTableBuilder symbolTable = new MathSymbolTableBuilder();
        instanceEnvironment.setSymbolTable(symbolTable);

        for (Iterator<File> i = files.iterator(); i.hasNext();) {
            File file = i.next();
            if (file.isDirectory()) {
                if (compileDirs) {
                    compileFilesInDir(file, instanceEnvironment);
                }
                else {
                    System.err.println("Skipping directory " + file.getName());
                }
            }
            else if (!isResolveFile(file.getName())) {
                System.err.println("The file " + file.getName()
                        + " is not a RESOLVE file.");
            }
            else if (!file.isFile()) {
                System.err.println("Cannot find the file " + file.getName()
                        + " in this directory.");
            }
            else {
                instanceEnvironment.setTargetFile(file);
                compileMainFile(file, instanceEnvironment, symbolTable);
            }
        }
        if (files.size() == 0) {
            if (instanceEnvironment.flags.isFlagSet(ResolveCompiler.FLAG_WEB)) {
                compileMainSource(inputFile, instanceEnvironment, symbolTable);
            }
        }
    }

    public static void compileFilesInDir(File dir,
            CompileEnvironment instanceEnvironment) {

        File[] fileArray = dir.listFiles();
        List<File> files = new List<File>();

        // JMH avoid problems with 1.5 generics files.addAll(fileArray);
        //        files.addAll(fileArray);
        for (int i = 0; i < fileArray.length; i++) {
            files.add(fileArray[i]);
        }
        MetaFile dummy = null;
        compileFiles(files, instanceEnvironment, dummy);
    }

    public static void compileMainFile(File file,
            CompileEnvironment instanceEnvironment,
            MathSymbolTableBuilder symbolTable) {

        Controller control = new Controller(instanceEnvironment);
        control.compileTargetFile(file, symbolTable);

        if (instanceEnvironment.showBuild()) {
            //           LOG.debug("showBuild flag set, printing module dec.");
            printModuleDec(file, instanceEnvironment);
        }
        else if (instanceEnvironment.showEnv()) {
            printEnvironment(file, instanceEnvironment);
        }
        else if (instanceEnvironment.showTable()
                || instanceEnvironment.showBind()) {
            //printSymbolTable(file, instanceEnvironment);
        }
    }

    public static void compileMainSource(MetaFile inputFile,
            CompileEnvironment instanceEnvironment,
            MathSymbolTableBuilder symbolTable) {

        Controller control = new Controller(instanceEnvironment);
        control.compileTargetSource(inputFile, symbolTable);

        /*if (env.showBuild()) {
        //           LOG.debug("showBuild flag set, printing module dec.");
            printModuleDec(file);
        } else if (env.showEnv()) {
            printEnvironment(file);
        } else if (env.showTable() || env.showBind()) {
            printSymbolTable(file);
        }*/
    }

    private static void printModuleDec(File file, CompileEnvironment env) {
        if (env.contains(file)) {
            ModuleDec dec = env.getModuleDec(env.getModuleID(file));
            System.out.println(dec.asString(0, 2));
        }
    }

    private static void printEnvironment(File file, CompileEnvironment env) {
        System.out.println();
        System.out.println(env.toString());
    }

    private static File getMainDir(String preferredMainDirectory) {
        File mainDir = null;

        if (preferredMainDirectory != null) {
            mainDir = new File(preferredMainDirectory);

            if (!mainDir.exists()) {
                System.err.println("Warning: Directory '"
                        + preferredMainDirectory
                        + "' not found, using current " + "directory.");

                mainDir = getAbsoluteFile("");
            }
        }
        else {
            File currentDir = getAbsoluteFile("");

            if (currentDir.getName().equals(mainDirName)) {
                mainDir = currentDir;
            }

            while ((mainDir == null) && (currentDir.getParentFile() != null)) {
                currentDir = currentDir.getParentFile();
                if (currentDir.getName().equals(mainDirName)) {
                    mainDir = currentDir;
                }
            }

            if (mainDir == null) {
                System.err.println("Warning: Directory '" + mainDirName
                        + "' not found, using current directory.");

                mainDir = getAbsoluteFile("");
            }
        }

        return mainDir;
    }

    /**
     * Converts the specified pathname to a <code>File</code> representing
     * the absolute path to the pathname.
     */
    private static File getAbsoluteFile(String pathname) {
        return new File(pathname).getAbsoluteFile();
    }

    /**
     * Determines if the specified filename is a valid Resolve filename.
     */
    private static boolean isResolveFile(String filename) {
        return (filename.endsWith(".mt") || filename.endsWith(".co")
                || filename.endsWith(".en") || filename.endsWith(".rb")
                || filename.endsWith(".fa") || filename.endsWith(".pp"));
    }

    private static void printHelpMessage(CompileEnvironment e) {
        System.out.println("Usage: java -jar RESOLVE.jar [options] <files>");
        System.out.println("where options include:");

        printOptions(e);
    }

    private static void printOptions(CompileEnvironment e) {
        System.out
                .println("  +bodies        Compile imported realization modules.");
        System.out
                .println("  -showBuild     Show the ModuleDec of the target file.");
        System.out
                .println("  -showEnv       Show the compilation environment.");
        System.out
                .println("  -showTable     Show the symbol table before binding.");
        System.out
                .println("  -showBind      Show the symbol table after binding.");
        System.out
                .println("  -showIndirect  Show the bindings associated with indirect types.");
        System.out.println("  -assertions    Print Only Final Assertions");
        System.out.println("  -R             Recurse through directories.");
        System.out.println("  -D <dir>       Use <dir> as the main directory.");
        System.out.println("  -translate     Translate to Java code.");
        System.out.println("  -PVCs           Generate verification "
                + "conditions for performance.");
        System.out.println("  -VCs           Generate verification "
                + "conditions.");
        System.out.println("  -isabelle      Used with -VCs to generate "
                + "VCs for Isabelle.");

        System.out.println(FlagDependencies.getListingString(e.flags
                .isFlagSet(FLAG_EXTENDED_HELP)));
    }

    /**
     * <p>This method sets up dependencies between compiler flags.  If you are
     * integrating your module into the compiler flag management system, this is
     * where to do it.</p>
     */
    private synchronized static void setUpFlagDependencies() {

        if (!FlagDependencies.isSealed()) {

            setUpFlags();
            Prover.setUpFlags();
            JavaTranslator.setUpFlags();
            CTranslator.setUpFlags();
            Archiver.setUpFlags();
            ResolveCompiler.setUpFlags();
            VCGenerator.setUpFlags();
            AlgebraicProver.setUpFlags();
            //Your module here!
            CongruenceClassProver.setUpFlags();
            SMTProver.setUpFlags();
            FlagDependencies.seal();
        }
    }

    private static void setUpFlags() {
        FlagDependencies.addImplies(FLAG_EXTENDED_HELP, FLAG_HELP);
    }

    private static HashMap<String, MetaFile> getFakeHashMap() {
        HashMap<String, MetaFile> aMap = new HashMap<String, MetaFile>();
        String key = "Unbounded_List_Template.Std_Unbounded_List_Realiz";
        String fileName = "Std_Unbounded_List_Raliz";
        String assocConcept = "Unbounded_List_Template";
        String pkg = "Unbounded_List";
        String source =
                "Realization Std_Unbounded_List_Realiz for Unbounded_List_Template;\n"
                        + "Type List is represented by Record\n"
                        + "Contents: Array 1..Max_Depth of Entry;\n"
                        + "end;\n"
                        + "convention\n"
                        + "true;\n"
                        +

                        "true;\n"
                        + "Procedure Advance( upd P: List );\n"
                        + "end;\n"
                        + "Procedure Reset( upd P: List );\n"
                        + "end;\n"
                        + "Procedure Is_Rem_Empty( rest P: List ): Boolean;\n"
                        + "end;\n"
                        + "Procedure Insert( alt New_Entry: Entry; upd P: List );\n"
                        + "end;\n"
                        + "Procedure Remove( rpl Entry_Removed: Entry; upd P: List );\n"
                        + "end;\n"
                        + "Procedure Advance_to_End( upd P: List );\n"
                        + "end;\n"
                        + "Procedure Swap_Remainders( upd P, Q: List );\n"
                        + "end;\n"
                        + "Procedure Is_Prec_Empty( rest P: List ): Boolean;\n"
                        + "end;\n" + "Procedure Clear( clr P: List );\n"
                        + "end;\n" + "end Std_Unbounded_List_Realiz;";
        ModuleKind kind = ModuleKind.CONCEPT_BODY;
        MetaFile mf = new MetaFile(fileName, assocConcept, pkg, source, kind);
        aMap.put(key, mf);
        key = "Stack.Do_Nothing_Realiz";
        fileName = "Do_Nothing_Realiz";
        assocConcept = "Stack_Template";
        pkg = "Stack";
        source =
                "Realization Do_Nothing_Realiz for Do_Nothing_Capability of Stack_Template;\n"
                        + "uses Std_Boolean_Fac;\n"
                        + "Procedure Do_Nothing(restores S: Stack);\n"
                        + "Var Next_Entry: Entry;\n" +

                        "Pop(Next_Entry, S);\n" + "Push(Next_Entry, S);\n"
                        + "end Do_Nothing;\n" + "end Do_Nothing_Realiz;\n";
        kind = ModuleKind.ENHANCEMENT_BODY;
        mf = new MetaFile(fileName, assocConcept, pkg, source, kind);
        aMap.put(key, mf);
        key = "User/Web_User.A_A0123456";
        fileName = "a_a0123456";
        assocConcept = "";
        pkg = "User/Web_User";
        source =
                "Facility a_a0123456;\n"
                        + "uses Std_Boolean_Fac, Std_Character_Fac, Std_Integer_Fac, Std_Char_Str_Fac;\n"
                        + "Facility Stack_Fac is Stack_Template(Integer, 4)\n"
                        + "realized by Array_Realiz\n"
                        + "enhanced by Reading_Capability\n"
                        + "realized by Obvious_Reading_Realiz(Std_Integer_Fac.Read)\n"
                        + "enhanced by Writing_Capability\n"
                        + "realized by Obvious_Writing_Realiz (Std_Integer_Fac.Write);\n"
                        +

                        "Operation Main();\n" + "Procedure\n"
                        + "Var S: Stack_Fac.Stack;\n" + "Read(S);\n"
                        + "Write_Line(\"REVERSED ORDER\");\n" + "Write(S);\n"
                        + "end Main;\n" + "end a_a0123456;\n";
        kind = ModuleKind.FACILITY;
        mf = new MetaFile(fileName, assocConcept, pkg, source, kind);
        aMap.put(key, mf);
        return aMap;
    }
}
