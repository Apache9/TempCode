package com.github.apache9.log4j2;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.config.Log4j1ConfigurationConverter;
import org.apache.logging.log4j.core.config.builder.impl.DefaultConfigurationBuilder;

/**
 * @author zhangduo
 */
public class HadoopLog4j1ConfigurationConverter {

	public static void main(String[] args) throws IOException {
		Log4j1ConfigurationConverter.CommandLineArguments cmdArgs = new Log4j1ConfigurationConverter.CommandLineArguments();
		cmdArgs.setVerbose(true);
		cmdArgs.setPathOut(Paths.get("/home/zhangduo/log4j2.xml"));
		Files.walkFileTree(Paths.get("/home/zhangduo/hadoop/code"), new FileVisitor<Path>() {

			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				String fileName = file.getFileName().toString();
				if (fileName.startsWith("log4j") && !fileName.startsWith("log4j2")
						&& fileName.endsWith(".properties")) {
					// this file will cause NPE, skip it.
					if (file.toAbsolutePath().toString().equals(
							"/home/zhangduo/hadoop/code/hadoop-tools/hadoop-dynamometer/hadoop-dynamometer-infra/src/test/resources/conf/etc/hadoop/log4j.properties")) {
						System.err.println("skip " + file);
						return FileVisitResult.CONTINUE;
					}
					cmdArgs.setPathIn(file);
					Log4j1ConfigurationConverter.run(cmdArgs);
					String logFor = "";
					if (fileName.length() > "log4j.properties".length()) {
						logFor = fileName.substring("log4j".length(), fileName.length() - ".properties".length());
					}
					Path outputFile = Paths.get(file.getParent().toString(), "log4j2" + logFor + ".xml");
					System.err.println("write to " + outputFile);
					try {
						DefaultConfigurationBuilder.formatXml(new StreamSource(cmdArgs.getPathOut().toFile()),
								new StreamResult(outputFile.toFile()));
					} catch (TransformerFactoryConfigurationError | TransformerException e) {
						throw new IOException(e);
					}
					if (!file.toFile().delete()) {
						System.err.println("failed to delete source file " + file);
					} else {
						System.err.println("deleted source file " + file);
					}
				}
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				return FileVisitResult.CONTINUE;
			}
		});
	}
}
