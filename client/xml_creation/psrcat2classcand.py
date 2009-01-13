#!/usr/bin/python
import sys

if len(sys.argv) < 2:
	print "needs a file!"
	sys.exit(1)

file = open(sys.argv[1])
print "<?xml version='1.0' ?>"
print '<?xml-stylesheet type="text/xsl" href="/web/xsl/ClassifiedCandidateIndex.xsl"?>'
print '<classified_candidate_index>'

for line in file:
	elems = line.split()
	name = elems[0]
	ra = elems[1]
	dec = elems[2]
	period = elems[3]
	dm = elems[4]

	print "<classified_candidate>"
	print "<cand_class_int>4</cand_class_int>"
	print "<coordinate>J%s %s</coordinate>"%(ra,dec)
	print "<name>PSR %s</name>"%name
	print "<raw_candidate_basic>"
	print "<bary_period>%s</bary_period>"%period
	print "<dm>%s</dm>"%dm
	print "<topo_period>%s</topo_period>"%period
	print "</raw_candidate_basic>"
	print "<possible_matches>"
	print "</possible_matches>"
	print "<confirmed_matches>"
	print "</confirmed_matches>"
	print "</classified_candidate>"

print '</classified_candidate_index>'

