(function() {
  var jast;
  jast = exports;
  require('./parser').populate(jast);
  require('./nodes').populate(jast);
  require('./walkers').populate(jast);
}).call(this);
