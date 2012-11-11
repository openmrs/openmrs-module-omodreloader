openmrs-module-omodreloader
===========================

Reloads modules whenever respective omods change in specified paths

You need to install the Omod Reloader Module and then start OpenMRS with:

-Domodreloader.paths="d:\workspace\metadatasharing\omod\target;d:\workspace\metadatamapping\omod\target"

If you provide a path to a directory, the module will be reloading any omods found in that directory.