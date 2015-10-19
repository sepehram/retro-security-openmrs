#!/bin/sh

mod_name=retrosecurity
root_path=..
rwt_path=$root_path/rewriter
out_path=$rwt_path/output
mod_path=$root_path/$mod_name
mod_src_path=$mod_path/api/src/main/java/org/openmrs/module/$mod_name
adv_path=$mod_src_path/advice
impl_path=$mod_src_path/api/impl
tar_path=$mod_path/omod/target
config_path=$mod_path/omod/src/main/resources


javac -classpath $rwt_path AdviceRewriter.java
java AdviceRewriter
cp $out_path/PatientServiceAdvice.java $adv_path
cp $out_path/EngineCommunication.java $mod_src_path
cp $out_path/LoggedCallDerivationListener.java $mod_src_path
cp $out_path/RetroSecurityServiceImpl.java $impl_path
cp $out_path/config.xml $config_path
cd $mod_path
mvn install
cp $tar_path/*.omod $rwt_path
rm $out_path/PatientServiceAdvice.java
rm $out_path/EngineCommunication.java
rm $out_path/LoggedCallDerivationListener.java
rm $out_path/RetroSecurityServiceImpl.java
rm $out_path/config.xml
rm $rwt_path/AdviceRewriter.class

