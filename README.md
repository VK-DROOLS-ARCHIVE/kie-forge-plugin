Using kie-forge-plugin
==========================

**Please install JBoss Forge version 1.4.4**

Quick start
===========

Type in the following commands into the forge console.

* install the plugin:

        $ forge git-plugin https://github.com/vinodkiran/kie-forge-plugin

* Start by setting up 'kie'

        $ kie setup
        $ kie kmodule

* Add a KieBase

        $ kie add-kbase --named sample_kbase --package com.acme.rules --default

    * --named: mandatory.

    * --package: optional. If missing the name is taken as the package

    * --default: optional. mark the kiebase as default

* Add a KieSession

        $ kie add-ksession --named ks1 --kbase sample_kbase --type stateless --default

    * --named: mandatory.

    * --kbase: mandatory. Pressing TAB will show list of all kbase's defined.

    * --type: optional. stateful or stateless. If missing, kiesession is assumed to be stateful.

    * --default: optional. mark the kiesession as default

* Generate Invocation Code (CDI)

        $ kie generate-cdi --named Invoker --package com.acme.rules

    * --named: mandatory.

    * --package: mandatory.

