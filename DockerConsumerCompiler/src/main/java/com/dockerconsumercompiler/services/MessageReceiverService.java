package com.dockerconsumercompiler.services;

import com.dockerconsumercompiler.vo.ProgramEntity;
import org.apache.commons.exec.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.*;
import java.nio.file.FileSystem;
import java.util.logging.Logger;

/**
 * Created by Manohar Prabhu on 5/31/2016.
 */
public class MessageReceiverService {
    Logger logger = Logger.getLogger(MessageReceiverService.class.getName());
    private ProgramRepository programRepository;

    @Autowired
    public MessageReceiverService(ProgramRepository programRepository) {
        this.programRepository = programRepository;
    }

    public void receiveMessage(String message) throws IOException {
        logger.info("Received message ID " + message + " from the producer");
        ProgramEntity programEntity = programRepository.findByQueueId(message);
        if(programEntity == null) {
            return;
        }

        DefaultExecutor defaultExecutor = new DefaultExecutor();

        //Create a temp directory of name queueID
        CommandLine makeDirectory = new CommandLine("mkdir");
        makeDirectory.addArgument(message);
        defaultExecutor.execute(makeDirectory);


        //write the program and input file into this
        PrintWriter programWriter = new PrintWriter(message + File.separator + "program");
        programWriter.write(programEntity.getProgram());
        programWriter.close();

        //Compile the program
        CommandLine compiler = CommandLine.parse("gcc -x c " + message + File.separator + "program" + " -o "+ message + File.separator + "a.out");
        try {
            defaultExecutor.execute(compiler);
        } catch (ExecuteException e) {
            logger.info("Compilation was UNSUCCESSFUL");
            e.printStackTrace();
            // Update database status code to PROGRAM_COMPILATION_ERROR = 3;
            programEntity.setProgramStatus(3);
            programEntity.setErrorMessage("Compilation error");
            programRepository.save(programEntity);
            return;
        }

        //If compilation succeeds, run the binary and pipe the input into it
        CommandLine executorCommand = CommandLine.parse(message + File.separator + "a.out");
        ByteArrayInputStream input =
                new ByteArrayInputStream(programEntity.getInput().getBytes("ISO-8859-1"));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        DefaultExecutor timedExecutor = new DefaultExecutor();
        timedExecutor.setExitValue(0);
        ExecuteWatchdog watchdog = new ExecuteWatchdog(2000);
        timedExecutor.setWatchdog(watchdog);
        try {
            timedExecutor.setStreamHandler(new PumpStreamHandler(output, null, input));
            timedExecutor.execute(executorCommand);
            programEntity.setProgramStatus(6);
            programEntity.setOutput(output.toString());
            programRepository.save(programEntity);
        } catch(ExecuteException e) {
            logger.info("Non-zero exit value. The program crashed \\ timedout");
            e.printStackTrace();
            // Update database status code to PROGRAM_EXECUTION_TIMEOUT = 4;
            programEntity.setProgramStatus(4);
            programEntity.setErrorMessage("Program did not complete execution in time");
            programRepository.save(programEntity);
            return;
        }
    }
}
