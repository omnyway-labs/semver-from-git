## Using npm libraries

### Async / Callback hell

Biggest problem is most npm libraries are async. The results of a function are
passed to a callback function. This makes it very difficult to do scripting
style programs that want to have each function block til done and return the
results.

It is possible to use core-async (with the andare version of core.async that
supports bootstrapped clojurescript). But you have to wrap the put channel
(`>!`) of the function in a go block and the script where you call the function
also has to wrap the `<!` in a go block. You can not use the blocking versions
of `>!!` and `<!!` and you can't use the clojure `promise` / `deliver`. This is
due to the fact that JS has no real threads.

So this means that you can't just wrap an async npm function with a go block,
the wrapper function would return the channel, not the result.

### npm interop can be weird

Some npm library function signatures are hard to figure out how to map into
clojurescript interop calls.
