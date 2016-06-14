package com.codecompiler;

import com.codecompiler.configuration.MongoConfiguration;
import com.codecompiler.configuration.RabbitMQConfiguration;
import com.codecompiler.vo.ProgramArguments;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.logging.Logger;

/**
 * Created by Manohar Prabhu(Axionet) on 5/27/2016.
 */
@SpringBootApplication
public class Application {
    private static Logger logger = Logger.getLogger(Application.class.toString());

    public static void main(String[] args) throws CmdLineException {
        setProgramParameters(parseArguments(args));
        SpringApplication.run(Application.class, args);
    }

    public static void setProgramParameters(ProgramArguments arguments) {
        MongoConfiguration.DATABASE_NAME = arguments.mongodbDatabase;
        MongoConfiguration.HOST = arguments.mongodbHost;
        RabbitMQConfiguration.MQ_HOST = arguments.rmqHost;
    }

    public static ProgramArguments parseArguments(String[] args) throws CmdLineException {
        ProgramArguments arguments = new ProgramArguments();
        CmdLineParser parser = new CmdLineParser(arguments);
        parser.parseArgument(args);
        return arguments;
    }
}
