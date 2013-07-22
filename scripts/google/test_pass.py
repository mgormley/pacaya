import getpass
print "WARNING: do not provide a real password!"
print "%s, enter your " % getpass.getuser(), 
pw1 = getpass.getpass()
print "Confirm ",
pw2 = getpass.getpass()

if pw1 == pw2:
    print 'Your password was "%s".' % pw1
else:
    print 'BAD: you provided different passwords "%s" and "%s".' % (pw1, pw2)
