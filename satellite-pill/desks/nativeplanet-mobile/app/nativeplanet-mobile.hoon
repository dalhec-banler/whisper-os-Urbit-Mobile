::  nativeplanet-mobile: the phone's side of one identity
::
::    Reports the mobile app inventory and mirrors the planet's
::    DMs through the planet's %satellite relay. The phone reads the mirror
::    over local Eyre and sends through it; the planet does the talking.
::
/+  default-agent, dbug
|%
+$  card  card:agent:gall
+$  state-1
  $:  %1
      parent=(unit ship)
      snap=json
      live=(list json)
      errors=(list json)
  ==
--
=|  state-1
=*  state  -
%-  agent:dbug
^-  agent:gall
=<
|_  =bowl:gall
+*  this  .
    def   ~(. (default-agent this %|) bowl)
    hc    ~(. +> bowl)
::
++  on-init
  ^-  (quip card _this)
  `this
::
++  on-save  !>(state)
++  on-load
  |=  old=vase
  ^-  (quip card _this)
  =/  ver  (mole |.(!<(state-1 old)))
  ?~  ver
    ~&  >>>  'nativeplanet-mobile: state reset'
    `this
  :_  this(state u.ver)
  ?~  parent.u.ver  ~
  ~[(watch-parent:hc u.parent.u.ver)]
::
++  on-poke
  |=  [=mark =vase]
  ^-  (quip card _this)
  ?>  =(src.bowl our.bowl)
  ?+    mark  (on-poke:def mark vase)
      %noun
    =+  !<([%pair p=ship] vase)
    :_  this(parent `p, snap ~, live ~, errors ~)
    ~[(watch-parent:hc p)]
  ::
      %json
    ?~  parent.state  ~|('not paired with a planet' !!)
    :_  this
    ~[[%pass /fwd %agent [u.parent.state %satellite] %poke %json vase]]
  ==
::
++  on-watch
  |=  =path
  ^-  (quip card _this)
  ?>  =(src.bowl our.bowl)
  ?+    path  (on-watch:def path)
      [%mirror ~]
    :_  this
    ~[[%give %fact ~ %json !>(mirror:hc)]]
  ==
::
++  on-leave  on-leave:def
::
++  on-agent
  |=  [=wire =sign:agent:gall]
  ^-  (quip card _this)
  ?+    wire  (on-agent:def wire sign)
      [%sat ~]
    ?+    -.sign  `this
        %kick
      :_  this
      ?~  parent.state  ~
      ~[(watch-parent:hc u.parent.state)]
    ::
        %fact
      ?.  =(%json p.cage.sign)  `this
      =/  jon=json  !<(json q.cage.sign)
      =/  new=_this
        ?.  ?=([%o *] jon)  this
        ?:  (~(has by p.jon) 'snapshot')
          this(snap (~(got by p.jon) 'snapshot'))
        ?:  (~(has by p.jon) 'chat')
          this(live (scag live-cap:hc `(list json)`[(~(got by p.jon) 'chat') live.state]))
        ?:  (~(has by p.jon) 'error')
          this(errors (scag error-cap:hc `(list json)`[(~(got by p.jon) 'error') errors.state]))
        this
      :_  new
      :-  [%give %fact ~[/mirror] %json !>((mirror-of:hc state.new))]
      ::  a fact about a DM thread the snapshot never mentioned: ask the
      ::  planet for a fresh snapshot so the thread list learns of it
      ?.  ?&  ?=([%o *] jon)
              (~(has by p.jon) 'chat')
              ?=(^ parent.state.new)
              (unknown-whom:hc (~(got by p.jon) 'chat') snap.state.new)
          ==
        ~
      =/  ask=json  (frond:enjs:format 'refresh' ~)
      ~[[%pass /fwd %agent [u.parent.state.new %satellite] %poke %json !>(ask)]]
    ==
  ==
::
++  on-peek
  |=  =path
  ^-  (unit (unit cage))
  ?+  path  ~
      [%x %apps ?(~ [%json ~])]    ``json+!>(apps-json:hc)
      [%x %mirror ?(~ [%json ~])]  ``json+!>(mirror:hc)
  ==
::
++  on-arvo  on-arvo:def
++  on-fail  on-fail:def
--
::
|_  =bowl:gall
::  live facts and relay errors kept, newest first
++  live-cap   500
++  error-cap  50
::
++  watch-parent
  |=  p=ship
  ^-  card
  [%pass /sat %agent [p %satellite] %watch /moon/chat]
::
::  +unknown-whom: does this chat fact name a DM the snapshot lacks?
++  unknown-whom
  |=  [fact=json snap=json]
  ^-  ?
  ?.  ?=([%o *] fact)  |
  =/  whom=(unit json)  (~(get by p.fact) 'whom')
  ?~  whom  |
  ?.  ?=([%s *] u.whom)  |
  ?.  ?=([%o *] snap)  &
  =/  writs=(unit json)  (~(get by p.snap) 'writs')
  ?~  writs  &
  ?.  ?=([%o *] u.writs)  &
  !(~(has by p.u.writs) p.u.whom)
::
++  mirror  (mirror-of state)
++  mirror-of
  |=  s=state-1
  ^-  json
  %-  pairs:enjs:format
  :~  ['parent' ?~(parent.s ~ s+(scot %p u.parent.s))]
      ['snapshot' snap.s]
      ['live' a+(flop live.s)]
      ['errors' a+(flop errors.s)]
  ==
::
++  apps-json
  ^-  json
  =/  app
    |=  [desk=@t recommended=?]
    %-  pairs:enjs:format
    :~  ['desk' s+desk]
        ['preferredLaunchMode' ~]
        ['androidPackage' ~]
        ['pwaManifestPath' ~]
        ['mobilePath' ~]
        ['recommended' b+recommended]
        ['hidden' b+|]
    ==
  %-  pairs:enjs:format
  :~  ['version' n+'1']
      :-  'apps'
      :-  %a
      :~  (app 'groups' &)
          (app 'webterm' &)
          (app 'landscape' |)
          (app 'grove' &)
          (app 'kin' &)
      ==
  ==
--
