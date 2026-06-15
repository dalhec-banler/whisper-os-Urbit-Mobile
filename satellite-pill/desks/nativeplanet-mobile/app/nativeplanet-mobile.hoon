^-  agent:gall
=<
|_  =bowl:gall
+*  this  .
::
++  on-init
  ^-  (quip card:agent:gall _this)
  `this
::
++  on-save
  !>(~)
::
++  on-load
  |=  old-state=vase
  ^-  (quip card:agent:gall _this)
  `this
::
++  on-poke
  |=  [=mark =vase]
  ^-  (quip card:agent:gall _this)
  `this
::
++  on-watch
  |=  =path
  ^-  (quip card:agent:gall _this)
  `this
::
++  on-leave
  |=  =path
  ^-  (quip card:agent:gall _this)
  `this
::
++  on-peek
  |=  =path
  ^-  (unit (unit cage))
  ?+  path  ~
      [%x %apps ~]
    ``json+!>(apps-json)
      [%x %apps %json ~]
    ``json+!>(apps-json)
  ==
::
++  on-agent
  |=  [=wire =sign:agent:gall]
  ^-  (quip card:agent:gall _this)
  `this
::
++  on-arvo
  |=  [=wire sign=sign-arvo]
  ^-  (quip card:agent:gall _this)
  `this
::
++  on-fail
  |=  [=term =tang]
  ^-  (quip card:agent:gall _this)
  `this
--
::
|_  =bowl:gall
++  apps-json
  ^-  json
  %-  pairs:enjs:format
  :~  ['version' [%n '1']]
      :-  'apps'
      :-  %a
        :~  %-  pairs:enjs:format
              :~  ['desk' [%s 'groups']]
                  ['preferredLaunchMode' [%s 'local_webview']]
                  ['androidPackage' ~]
                  ['pwaManifestPath' ~]
                  ['mobilePath' [%s '/apps/groups/']]
                  ['recommended' [%b &]]
                  ['hidden' [%b |]]
              ==
            %-  pairs:enjs:format
              :~  ['desk' [%s 'webterm']]
                  ['preferredLaunchMode' [%s 'local_webview']]
                  ['androidPackage' ~]
                  ['pwaManifestPath' ~]
                  ['mobilePath' [%s '/apps/webterm/']]
                  ['recommended' [%b &]]
                  ['hidden' [%b |]]
              ==
            %-  pairs:enjs:format
              :~  ['desk' [%s 'landscape']]
                  ['preferredLaunchMode' [%s 'local_webview']]
                  ['androidPackage' ~]
                  ['pwaManifestPath' ~]
                  ['mobilePath' [%s '/apps/landscape/']]
                  ['recommended' [%b |]]
                  ['hidden' [%b |]]
              ==
            %-  pairs:enjs:format
              :~  ['desk' [%s 'grove']]
                  ['preferredLaunchMode' [%s 'local_webview']]
                  ['androidPackage' ~]
                  ['pwaManifestPath' ~]
                  ['mobilePath' [%s '/apps/grove/']]
                  ['recommended' [%b &]]
                  ['hidden' [%b |]]
              ==
            %-  pairs:enjs:format
              :~  ['desk' [%s 'kin']]
                  ['preferredLaunchMode' [%s 'local_webview']]
                  ['androidPackage' ~]
                  ['pwaManifestPath' ~]
                  ['mobilePath' [%s '/apps/kin/']]
                  ['recommended' [%b &]]
                  ['hidden' [%b |]]
              ==
        ==
  ==
--
