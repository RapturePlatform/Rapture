
# Instructions notes about generating the Slate documentation



## why not jruby?
Redcarpet is the only markdown lib that i have tried that renders the markdown correctly into html. 
Unfortunately it requires C extensions which rules out jruby. 

## Dependencies
Ruby

Bundler
Middleman
Rake


gem install rake
gem install middleman
gem install bundler


## To Run
to run it manually:
rake build

or the gradle command:
gradle publishSlate

N.B. that you PATH must be setup correctly for the gradle task to work correctly



