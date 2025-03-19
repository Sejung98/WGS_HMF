package com.hartwig.hmftools.sigs.nmf;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

public class NmfConfig {

    // the set number of signatures to fit
    final public int SigCount;

    // how many additional sigs can be found
    final public int SigExpansionCount;

    // number of runs, literally trials with different initial matrix seeding
    final public int RunCount;

    // exit point for search
    final public int MaxIterations;

    // the minimum objective value on which to stop evaluation
    final public double ExitLevel;

    final public NmfModelMethod Model;

    // a set of signatures with which to seed the NMF runs
    final public String RefSigFilename;
    final public boolean UseRefSigs; // if false, then they're only provided for post-run comparisons
    final public String RefContribFilename;

    // applicable to ref or pre-discovery input sigs - how much they be adjusted during the NMF routine,
    // where 0 means not at all, 1 as per the discovered sigs
    final public double SigFloatRate;

    final public boolean FitOnly; // apply fitting routine to samples using input/ref sigs
    final public boolean FitRestrictToContribs; // only allow ref sigs if allocated already
    final public boolean ApplyPcawgRules; // when fitting to PCAWG sigs, apply their rules for inclusion and exclusion

    final public boolean LogVerbose;

    // command line args
    public static final String NMF_SIG_COUNT = "nmf_sig_count";
    public static final String NMF_RUN_COUNT = "nmf_run_count";
    public static final String NMF_MAX_ITERATIONS = "nmf_max_iterations";
    public static final String NMF_EXIT_LEVEL = "nmf_exit_level";
    public static final String NMF_REF_SIG_FILE = "nmf_ref_sig_file";
    public static final String NMF_USE_REF_SIGS = "nmf_use_ref_sigs";
    public static final String NMF_REF_CONTRIB_FILE = "nmf_ref_contrib_file";
    public static final String NMF_SIG_FLOAT_RATE = "nmf_sig_float_rate";
    public static final String NMF_FIT_ONLY = "nmf_fit_only";
    public static final String NMF_PCAWG_RULES = "nmf_apply_pcawg_rules";
    public static final String NMF_FIT_RESTRICTED = "nmf_fit_restricted";

    public static final String NMF_SIG_EXPANSION = "nmf_sig_exp_count";

    public static final String NMF_LOG_VERBOSE = "nmf_log_verbose";

    public static final String NMF_MODEL = "nmf_model";

    public enum NmfModelMethod {
        STANDARD,
        BRUNET
    }

    public static final String NMF_MODEL_STD_STR = "Standard";
    public static final String NMF_MODEL_BRUNET_STR = "Brunet";

    public static void addCmdLineArgs(Options options)
    {
        options.addOption(NMF_SIG_COUNT, true, "Signatures count");
        options.addOption(NMF_RUN_COUNT, true, "Number of runs");
        options.addOption(NMF_MAX_ITERATIONS, true, "Max iterations");
        options.addOption(NMF_EXIT_LEVEL, true, "Exit level for cost function");
        options.addOption(NMF_MODEL, true, "NMF model");
        options.addOption(NMF_REF_SIG_FILE, true, "Option reference sig file");
        options.addOption(NMF_USE_REF_SIGS, false, "If true use reference sig file, otherwise only used for comparison");
        options.addOption(NMF_REF_CONTRIB_FILE, true, "Option reference contributions file");
        options.addOption(NMF_SIG_FLOAT_RATE, true, "How much any pre-discovery sig can float on each adjustment");
        options.addOption(NMF_PCAWG_RULES, false, "Apply PCAWG signature rules");
        options.addOption(NMF_SIG_EXPANSION, true, "Max number of sigs to expand to");
        options.addOption(NMF_FIT_ONLY, false, "Fit to input ref sigs, apply min-sig logic");
        options.addOption(NMF_FIT_RESTRICTED, false, "Fit to input ref sigs if has ref contribution");

        options.addOption(NMF_LOG_VERBOSE, false, "All NMF details logged");
    }

    public NmfConfig(final CommandLine cmd)
    {
        ExitLevel = Double.parseDouble(cmd.getOptionValue(NMF_EXIT_LEVEL));
        RunCount = Integer.parseInt(cmd.getOptionValue(NMF_RUN_COUNT));
        SigCount = Integer.parseInt(cmd.getOptionValue(NMF_SIG_COUNT));

        if(cmd.hasOption(NMF_MAX_ITERATIONS))
            MaxIterations = Integer.parseInt(cmd.getOptionValue(NMF_MAX_ITERATIONS));
        else
            MaxIterations = 100;

        final String modelStr = cmd.getOptionValue(NMF_MODEL);

        if(modelStr != null)
        {
            if(modelStr.equals(NMF_MODEL_BRUNET_STR))
                Model = NmfModelMethod.BRUNET;
            else
                Model = NmfModelMethod.STANDARD;
        }
        else
        {
            Model = NmfModelMethod.STANDARD;
        }

        RefSigFilename = cmd.hasOption(NMF_REF_SIG_FILE) ? cmd.getOptionValue(NMF_REF_SIG_FILE) : "";
        UseRefSigs = cmd.hasOption(NMF_USE_REF_SIGS);
        ApplyPcawgRules = cmd.hasOption(NMF_PCAWG_RULES);
        RefContribFilename = cmd.hasOption(NMF_REF_CONTRIB_FILE) ? cmd.getOptionValue(NMF_REF_CONTRIB_FILE) : "";
        FitOnly = cmd.hasOption(NMF_FIT_ONLY);
        FitRestrictToContribs = cmd.hasOption(NMF_FIT_RESTRICTED);

        SigExpansionCount = cmd.hasOption(NMF_SIG_EXPANSION) ? Integer.parseInt(cmd.getOptionValue(NMF_SIG_EXPANSION)) : 0;
        SigFloatRate = cmd.hasOption(NMF_SIG_FLOAT_RATE) ? Double.parseDouble(cmd.getOptionValue(NMF_SIG_FLOAT_RATE)) : 1.0;

        LogVerbose = cmd.hasOption(NMF_LOG_VERBOSE);
    }

    public NmfConfig(double exitLevel, int maxIterations)
    {
        ExitLevel = exitLevel;
        MaxIterations = maxIterations;

        RunCount = 0;
        SigCount = 0;
        FitOnly = true;
        UseRefSigs = true;
        LogVerbose = false;
        SigFloatRate = 0;
        SigExpansionCount = 0;
        FitRestrictToContribs = false;
        RefContribFilename = "";
        RefSigFilename = "";
        Model = NmfModelMethod.STANDARD;
        ApplyPcawgRules = false;
    }

}
