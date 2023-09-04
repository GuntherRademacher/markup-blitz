![Markup Blitz][logo]

Markup Blitz is a [parser generator][parser-generator] implemented in Java. It takes a [context-free grammar][CFG], generates a [parser][parser] for it, and uses the parser to convert input into a [parse tree][parse-tree], provided that the input conforms to the grammar.

The algorithms used are
* [LALR(1)][LALR] for parser table construction, and
* [GLR][GLR] for dynamic conflict resolution.

It also uses concepts from [REx Parser Generator][REx].

Markup Blitz is an implementation of [Invisible XML][IXML] (ixml). Please see the ixml specification for details on grammar notation and parse-tree serialization.

## Building Markup Blitz

Use JDK 11 or higher. For building Markup Blitz, use these commands:

```sh
git clone https://github.com/GuntherRademacher/markup-blitz.git
cd markup-blitz 
gradlew clean jar
```

This creates `build\libs\markup-blitz.jar` which serves the Markup Blitz API. It is also usable as an executable jar for standalone execution.

## Markup Blitz in Eclipse

The project can be imported into Eclipse as a Gradle project.

## Running Markup Blitz from command line

Markup Blitz can be run from command line to process some input according to an Invisible XML grammar:

```sh
Usage: java -jar markup-blitz.jar [<OPTION>...] <GRAMMAR> <INPUT>

  Compile an Invisible XML grammar, and parse input with the resulting parser.

  <GRAMMAR>          the grammar (file name or URL).
  <INPUT>            the input (file name or URL).

  Options:
    --verbose, -v    print intermediate results (to standard output).
    --timing         print timing information (to standard output).
    --indent, -i     generate resulting xml with indentation.
    --trace          print parser trace (to standard error).
    --help, -h, -?   print this information.
```

## Performance

As with [REx Parser Generator][REx], the goal of Markup Blitz is to provide good performance. In general, however, REx parsers can be expected to perform much better. This is primarily because REx allows separating the specification into tokenization and parsing steps. This is in contrast to Invisible XML, which uses a uniform grammar to resolve from the start symbol down to codepoint level. Separate tokenization enables the use of algorithms optimized for this purpose, the establishment of token termination rules, and the easy accommodation of whitespace rules. Without it, all this has to be accomplished by the parser, which often leads to costly handling of local ambiguities.

Some performance comparison of REx-generated parsers and Invisible XML parsers can be found in the [rex-parser-benchmark][rex-parser-benchmark] project. It will soon be extended to cover Markup Blitz as well.

## License

Markup Blitz is provided under the [Apache 2 License][ASL].

## Thanks

The work in this project was supported by the [BaseX][BaseX] organization.

[<img src="https://avatars.githubusercontent.com/u/621314?s=200&v=4" alt="drawing" width="40"/>][BaseX]

[logo]: markup-blitz.svg "Markup Blitz"
[BaseX]: https://basex.org/
[ASL]: http://www.apache.org/licenses/LICENSE-2.0
[REx]: https://bottlecaps.de/rex
[LALR]: https://en.wikipedia.org/wiki/LALR_parser
[GLR]: https://en.wikipedia.org/wiki/GLR_parser
[rex-parser-benchmark]: https://github.com/GuntherRademacher/rex-parser-benchmark
[IXML]: https://invisiblexml.org/
[CFG]: https://en.wikipedia.org/wiki/Context-free_grammar
[parser]: https://en.wikipedia.org/wiki/Parsing#Parser
[parse-tree]: https://en.wikipedia.org/wiki/Parse_tree
[parser-generator]: https://en.wikipedia.org/wiki/Compiler-compiler
