FROM ubuntu:14.04
MAINTAINER Kasper Luckow <kasper.luckow@sv.cmu.edu>

#############################################################################
# Setup base image 
#############################################################################
RUN \
  apt-get update -y && \
  apt-get install software-properties-common -y && \
  echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | debconf-set-selections && \
  add-apt-repository ppa:webupd8team/java -y && \
  apt-get update -y && \
  apt-get install -y oracle-java8-installer
# Cut it in two---java takes a long time to install
RUN  apt-get install -y \
                        unzip \
                        ant \
                        build-essential \
                        mercurial \
                        git \
                        vim && \
  rm -rf /var/lib/apt/lists/* && \
  rm -rf /var/cache/oracle-jdk8-installer

#############################################################################
# Environment 
#############################################################################

# set java env
ENV JAVA_HOME /usr/lib/jvm/java-8-oracle
ENV JUNIT_HOME /usr/share/java

# Make dir for distribution
WORKDIR /
RUN mkdir tools
ENV TOOLS_ROOT /tools


#############################################################################
# Install and configure jpf-related tools 
#############################################################################

# Set up jpf conf initially
RUN mkdir /root/.jpf
RUN echo "jpf-core = ${TOOLS_ROOT}/jpf-core" >> /root/.jpf/site.properties
RUN echo "jpf-symbc = ${TOOLS_ROOT}/jpf-symbc" >> /root/.jpf/site.properties
RUN echo "canopy = ${TOOLS_ROOT}/canopy" >> /root/.jpf/site.properties

# Set extensions var
RUN echo "extensions=\${jpf-core},\${jpf-symbc}" >> /root/.jpf/site.properties

# Install jpf-core
WORKDIR ${TOOLS_ROOT}
RUN hg clone http://babelfish.arc.nasa.gov/hg/jpf/jpf-core

WORKDIR ${TOOLS_ROOT}/jpf-core
# Update to version known to work
RUN hg update -r32
RUN ant

# Install jpf-symbc
WORKDIR ${TOOLS_ROOT}
RUN hg clone http://babelfish.arc.nasa.gov/hg/jpf/jpf-symbc

WORKDIR ${TOOLS_ROOT}/jpf-symbc
# Update to version known to work
#RUN hg update -r613
RUN ant

# Finally, get Canopy
# TODO: When we host this publicly, just clone it
WORKDIR ${TOOLS_ROOT}
# Kind of a hack: copy current dir (canopy) into $TOOLS_ROOT
ADD . ${TOOLS_ROOT}/canopy

WORKDIR ${TOOLS_ROOT}/canopy
# TODO: Update to version known to work
# RUN git checkout <VERSION>

# Get Canopy deps
RUN ant bootstrap
RUN ant resolve

# Build Canopy
RUN ant

# Update to new version of z3. Probably not strictly necessary
WORKDIR ${TOOLS_ROOT}

# Note that we specify a specific *release* of Z3
RUN wget https://github.com/Z3Prover/z3/releases/download/z3-4.4.1/z3-4.4.1-x64-ubuntu-14.04.zip 
RUN unzip z3-4.4.1-x64-ubuntu-14.04.zip && \
        rm z3-4.4.1-x64-ubuntu-14.04.zip
RUN ln -s z3-4.4.1-x64-ubuntu-14.04 z3

# Update LD_LIBRARY_PATH
ENV LD_LIBRARY_PATH ${TOOLS_ROOT}/z3/bin

# Copy z3 java bindings
RUN rm ${TOOLS_ROOT}/jpf-symbc/lib/com.microsoft.z3.jar
RUN cp ${TOOLS_ROOT}/z3/bin/com.microsoft.z3.jar ${TOOLS_ROOT}/jpf-symbc/lib/

# Let's go!
WORKDIR ${TOOLS_ROOT}