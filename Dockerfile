FROM ubuntu:focal

# install dependencies
RUN apt-get update -y && apt-get install openjdk-11-jre-headless lsof mpich zip python-is-python2 -y

# set a directory for the app
WORKDIR /pakrunner

# copy all the files to the container
COPY . .

# define the port number the container should expose
EXPOSE 8080

# run the command
CMD ["java", "-jar", "target/pakrunner-0.0.1-SNAPSHOT.jar"]

