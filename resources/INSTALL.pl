#! /usr/bin/perl

$| = 1;
use strict;
use warnings;
use File::Spec;      ## A standard file-handling library

########################################################################
#
#  Copyright (c) 2012. The Trustees of Columbia University in the City of New York.
#  The copyright owner has no objection to the reproduction of this work by anyone for
#  non-commercial use, but otherwise reserves all rights whatsoever.  For avoidance of
#  doubt, this work may not be reproduced, or modified, in whole or in part, for commercial
#  use without the prior written consent of the copyright owner.
#
########################################################################
#
# MADAMIRA uses the morphological databases of the Standard Arabic Morphological
# Analyzer (SAMA 3.1) form the Linguistic Data Consortium.  Due to licensing concerns,
# MADAMIRA cannot, at this time, include the SAMA 3.1 database files.
#
# To use MADAMIRA:
#
#
#(1) acquire SAMA 3.1 from the LDC first  (http://www.ldc.upenn.edu/, catalog number LDC2009E73).
#	and untar the file.
#
#(2) go to MADAMIRA-release-*/resources directory
#
#(3) run the following installation script once:
#	perl INSTALL.pl dir=<location of SAMA 3.1 parent directory>
#
########################################################################


############################################################################
##### READ COMMAND LINE  #####

my $dir="";      # initialize to empty string

my $madahome="../";

foreach( @ARGV ) {
	if( /^dir=(\S+)/ )   { $dir=$1; }
}

if( $dir eq "" ) {
    &printUsage();
    die "$0: Error - this install script requires that you specify a SAMA top-level directory. See usage above.";
}


############################################################################
##### VERIFY EXISTENCE OF DIRECTORIES TO VERIFY THAT THIS IS A VALID MADAMIRA BUILD  #####

if( $dir ne "" ) {
    $dir = File::Spec->rel2abs( $dir );              ## Gets absolute path to SAMA DIR
}


print "\n\n===============================================\n";
print "Running MADAMIRA resource installation script ...\n\n";

if( ! -d "$madahome/config" ||
    ! -d "$madahome/resources" ||
    ! -d "$madahome/third-party" ) {

    &printUsage();
    die "$0: Error - the current directory does not appear to be the MADAMIRA resources directory. This script must be run from that directory.\n";

}
else {

    print "Verified presence of MADAMIRA ...\n";
}


############################################################################
### HANDLE CONVERSION FOR EACH DIR PROVIDED

my $almordb = ""; ##  Output File Name

if( $dir ne "" ) {

    $almordb = "$madahome/resources/almor-s31.db";

    &buildALMORFile($dir, "SAMA3.1", $almordb);

}

print "===============================================\n";
print " Installation of MADAMIRA resources complete.  \n";
print "===============================================\n\n\n";


############################################################################
############################################################################
############################################################################
############################################################################



## Print usage information
sub printUsage {


    print "=================================================================================\n";
    print "Usage:  perl INSTALL.pl dir=<location of SAMA 3.1 parent directory>\n\n";

    print " The specified directory should be the top-level directory\n";
    print "     of the LDC SAMA 3.1 corpora (catalog number LDC2009E73)\n\n";
    print "==================================================================================\n\n";
}




## Build and place the ALMOR DB file
sub buildALMORFile {
    # acquire function arguments
    my ($dir, $type, $almordb) = @_;

    if( $almordb eq "" ) {
        ## This should never happen
        die "$0: Error - Did not understand what ALMOR database type to make ... \n";
    }

    ############################################################################
    ## From top level of corpora, descend down to directory containing dict* and table* files

    if( $type eq "SAMA3.1") {
        ## From top level of SAMA corpora
        ##  adjust downward to the directory containing the dict* and table* files:
        $dir = $dir . "/SAMA-3.1/lib/SAMA_DB/v3_1";

    }


    ############################################################################
    ## Verify the presence of dict* and table* files

    if( ! -d $dir ||
        ! -e "$dir/dictPrefixes" ||
        (! -e "$dir/dictStems" && ! -e "$dir/dictStems.tsv") ||
        ! -e "$dir/dictSuffixes" ||
        ! -e "$dir/tableAB" ||
        ! -e "$dir/tableAC" ||
        ! -e "$dir/tableBC" ) {

        &printUsage();
        die "$0: Error - The specified directory does not appear to exist, or does not contain all 3 dict* file and all 3 table* files under $dir.\n";

    }
    else {
        print "Located prefix and table directory $dir ...\n";
    }

    ############################################################################
    ##  Get SAMA map file
    my $map = "";
    if( $type eq "SAMA3.1") {
        $map = $madahome . "/resources/Form2Func-SAMA3.1.map";
    }

    if( $map eq "" || ! -e $map ) {
        die "$0: Error - Could not locate the map file: $map ... check that all of the MADAMIRA home directory is intact. \n";
    }
    print "...Finished locating components.\n\n";



    ############################################################################
    #####  CREATE ALMOR.db file from SAMA information  #####

    print "\n===============================================\n";

    my $cmd;

    ## Define the command to run, including the directory containing Perl libraries
    $cmd = "perl -I $madahome/resources $madahome/resources/XAMA-to-ALMOR3.pl $map $dir $almordb";


    print "\nCreating ALMOR database; there many be a few warning messages \n";
    print "here that can be ignored:\n   $cmd\n";

    system($cmd);  ## Runs the Perl  XAMA-to-ALMOR.pl script

    if( ! -e $almordb || -z $almordb ) {
        die "$0:  Error - Problem creating ALMOR database $almordb \n";
    }

}