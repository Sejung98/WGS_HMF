package com.hartwig.hmftools.svtools.sequence;

import static java.lang.Math.min;

import static com.hartwig.hmftools.common.genome.refgenome.RefGenomeSource.REF_GENOME;
import static com.hartwig.hmftools.common.genome.refgenome.RefGenomeSource.addRefGenomeConfig;
import static com.hartwig.hmftools.common.utils.config.ConfigUtils.addLoggingOptions;
import static com.hartwig.hmftools.common.utils.config.ConfigUtils.setLogLevel;
import static com.hartwig.hmftools.common.utils.Strings.reverseString;
import static com.hartwig.hmftools.common.utils.file.FileWriterUtils.addOutputOptions;
import static com.hartwig.hmftools.common.utils.file.FileWriterUtils.closeBufferedWriter;
import static com.hartwig.hmftools.common.utils.file.FileWriterUtils.createBufferedWriter;
import static com.hartwig.hmftools.common.utils.file.FileWriterUtils.parseOutputDir;
import static com.hartwig.hmftools.svtools.simulation.ShatteringConfig.registerConfig;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import com.hartwig.hmftools.common.genome.refgenome.RefGenomeInterface;
import com.hartwig.hmftools.common.genome.refgenome.RefGenomeSource;
import com.hartwig.hmftools.common.utils.config.ConfigBuilder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;

public class RefSequenceGenerator
{
    private RefGenomeInterface mRefGenome;
    private final String mOutputDir;
    private final String mKmerInputFile;
    private final String mBedFile;

    private static final Logger LOGGER = LogManager.getLogger(RefSequenceGenerator.class);

    private static final boolean mWriteFastaFormat = true;
    private static final int FASTA_LINE_LIMIT = 60;

    public RefSequenceGenerator(final ConfigBuilder configBuilder)
    {
        mOutputDir = parseOutputDir(configBuilder);
        mBedFile = configBuilder.getValue(BED_FILE);
        mKmerInputFile = configBuilder.getValue(KMER_INPUT_FILE);

        try
        {
            final String refGenomeFile = configBuilder.getValue(REF_GENOME);
            IndexedFastaSequenceFile refGenome = new IndexedFastaSequenceFile(new File(refGenomeFile));
            mRefGenome = new RefGenomeSource(refGenome);
        }
        catch(IOException e)
        {
            LOGGER.error("failed to load ref genome: {}", e.toString());
        }
    }

    public void run()
    {
        if(mOutputDir == null || mOutputDir.isEmpty())
        {
            LOGGER.error("invalid output directory");
            System.exit(1);
        }

        if(mKmerInputFile != null)
        {
            generateKmerData();
        }

        if(mBedFile != null)
        {
            generateRefGenomeSequences();
        }
    }

    private void generateRefGenomeSequences()
    {
        try
        {
            BufferedWriter writer;

            String outputFileName = mOutputDir;

            if(mWriteFastaFormat)
                outputFileName += "REF_GENOME_SEQUENCES.fastq";
            else
                outputFileName += "REF_GENOME_SEQUENCES.csv";

            writer = createBufferedWriter(outputFileName, false);

            if(!mWriteFastaFormat)
            {
                writer.write("SequenceName,Chromosome,PosStart,PosEnd,Sequence");
                writer.newLine();
            }

            final List<String> bedFileData = Files.readAllLines(new File(mBedFile).toPath());
            String currentChromosome = "";

            for(String line : bedFileData)
            {
                String[] bedInfo = line.split("\t");

                if(bedInfo.length < 3)
                {
                    LOGGER.error("invalid bed file line: {}", line);
                    return;
                }

                String chromosome = bedInfo[0];
                int posStart = Integer.parseInt(bedInfo[1]);
                int posEnd = Integer.parseInt(bedInfo[2]);
                String seqName = bedInfo.length >= 4 ? bedInfo[3] : "";

                LOGGER.debug("writing ref-genome sequence for {}: for chr({}) pos({} -> {})", seqName, chromosome, posStart, posEnd);

                final String reqSequence = mRefGenome.getBaseString(chromosome, posStart, posEnd);

                if(mWriteFastaFormat)
                {
                    if(!currentChromosome.equals(chromosome))
                    {
                        currentChromosome = chromosome;
                        writer.write(String.format(">%s", chromosome));
                        writer.newLine();
                    }

                    int sequenceLength = reqSequence.length();

                    int index = 0;
                    while(index < sequenceLength)
                    {
                        int endIndex = min(index + FASTA_LINE_LIMIT, sequenceLength);
                        writer.write(reqSequence.substring(index, endIndex));
                        writer.newLine();
                        index += FASTA_LINE_LIMIT;
                    }
                }
                else
                {

                    writer.write(String.format("%s,%s,%d,%d,%s",
                            seqName, chromosome, posStart, posEnd, reqSequence));
                    writer.newLine();
                }
            }

            closeBufferedWriter(writer);

        }
        catch(IOException exception)
        {
            LOGGER.error("Failed to read bed file({})", mBedFile);
        }
    }

    private static final int COL_KMER_ID = 0;
    private static final int COL_CHR = 1;
    private static final int COL_POS_START = 2;
    private static final int COL_ORIENT_START = 3;
    private static final int COL_POS_END = 4;
    private static final int COL_ORIENT_END = 5;
    private static final int COL_BASE_LENGTH = 6;
    private static final int COL_INSERT_SEQ = 7;

    private void generateKmerData()
    {
        if(mOutputDir.isEmpty() || mKmerInputFile.isEmpty())
            return;

        try
        {
            BufferedWriter writer;

            final String outputFileName = mOutputDir + "LNX_KMER_STRINGS.csv";

            writer = createBufferedWriter(outputFileName, false);
            writer.write("KmerId,Chromosome,PosStart,OrientStart,PosEnd,OrientEnd,KmerPosStrand,KmerNegStrand");
            writer.newLine();

            BufferedReader fileReader = new BufferedReader(new FileReader(mKmerInputFile));

            String line = fileReader.readLine(); // skip header

            while ((line = fileReader.readLine()) != null)
            {
                // parse CSV data
                String[] items = line.split(",", -1);

                if(items.length < COL_INSERT_SEQ+1)
                {
                    LOGGER.warn("invalid input string: {}", line);
                    continue;
                }

                final String kmerId = items[COL_KMER_ID];
                final String chromosome = items[COL_CHR];
                int posStart = Integer.parseInt(items[COL_POS_START]);
                int orientStart = Integer.parseInt(items[COL_ORIENT_START]);
                int posEnd = Integer.parseInt(items[COL_POS_END]);
                int orientEnd = Integer.parseInt(items[COL_ORIENT_END]);
                int baseLength = Integer.parseInt(items[COL_BASE_LENGTH]);
                final String insertSeq = items[COL_INSERT_SEQ];

                LOGGER.debug("producing KMER({}) for chr({}) pos({} -> {})", kmerId, chromosome, posStart, posEnd);

                final String kmerStringStart = getBaseString(chromosome, posStart, orientStart, baseLength);
                final String kmerStringEnd = getBaseString(chromosome, posEnd, orientEnd, baseLength);
                final String kmerPosStrand = kmerStringStart + insertSeq + kmerStringEnd;
                final String kmerNegStrand = reverseString(kmerPosStrand);

                writer.write(String.format("%s,%s,%d,%d,%d,%d,%s,%s",
                        kmerId, chromosome, posStart, orientStart, posEnd, orientEnd, kmerPosStrand, kmerNegStrand));
                writer.newLine();
            }

            closeBufferedWriter(writer);

        }
        catch(IOException exception)
        {
            LOGGER.error("Failed to read k-mer CSV file({})", mKmerInputFile);
        }
    }

    private final String getBaseString(final String chromosome, int position, int orientation, int length)
    {
        if(orientation == 1)
            return mRefGenome.getBaseString(chromosome, position - length, position);
        else
            return mRefGenome.getBaseString(chromosome, position, position + length);
    }

    private static final String KMER_INPUT_FILE = "kmer_input_file";
    private static final String BED_FILE = "bed_file";

    public static void main(@NotNull final String[] args)
    {
        ConfigBuilder configBuilder = new ConfigBuilder();
        configBuilder.addConfigItem(KMER_INPUT_FILE, true, "File specifying locations for which to produce K-mers");
        configBuilder.addConfigItem(BED_FILE, true, "File specifying locations for which to produce ref-genome sequences");
        addRefGenomeConfig(configBuilder, true);
        addLoggingOptions(configBuilder);
        addOutputOptions(configBuilder);

        configBuilder.checkAndParseCommandLine(args);

        setLogLevel(configBuilder);

        RefSequenceGenerator refSequenceGenerator = new RefSequenceGenerator(configBuilder);
        refSequenceGenerator.run();
    }
}
