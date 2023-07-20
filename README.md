![Markup Blitz][logo]

Markup Blitz is a Java implementation of Invisible XML as defined on <span style="font-family: monospace">https://invisiblexml.org/</span>

The algorithms used are
* [LALR(1)][LALR] for parser table construction, and
* [GLR][GLR] for dynamic conflict resolution.

Markup Blitz is still under development. 

Markup Blitz also uses concepts from [REx Parser Generator][REx], and as with REx, the goal is to provide good performance. In general, however, REx parsers can be expected to perform much better. This is primarily because REx allows separating the specification into tokenization and parsing steps. This is in contrast to Invisible XML, which uses a uniform grammar to resolve from the start symbol down to codepoint level. Separate tokenization enables the use of algorithms optimized for this purpose, the establishment of token termination rules, and the easy accommodation of whitespace rules. Without it, all this has to be accomplished by the parser, which often leads to costly handling of local ambiguities.

Some performance comparison of REx-generated parsers and Invisible XML parsers can be found in the [rex-parser-benchmark][rex-parser-benchmark] project. It will soon be extended to cover Markup Blitz as well.

## Building Markup Blitz

Use JDK 11 or higher. For building Markup Blitz, use these commands:

```sh
git clone https://github.com/GuntherRademacher/markup-blitz.git
cd markup-blitz 
gradlew clean build
```

This creates `build\libs\markup-blitz.jar` which serves the Markup Blitz API. It is also usable as an executable jar for standalone execution.

## Markup Blitz in Eclipse

The project can be imported into Eclipse as a Gradle project.

## Running Markup Blitz from command line

Markup Blitz can be run from command line to process some input according to an Invisible XML grammar:

```sh
Usage: java de.bottlecaps.markup.Blitz [<OPTION>...] <GRAMMAR> <INPUT>

  compile an Invisible XML grammar, and parse input with the resulting parser.

  <GRAMMAR>          the grammar (file name or URL).
  <INPUT>            the input (file name or URL).

  Options:
    -v, --verbose    print intermediate results (to standard output).
    -t, --trace      print parser trace (to standard error).
    -?, -h, --help   print this information.
```

## License

Markup Blitz is provided under the [Apache 2 License][ASL].

[logo]: markup-blitz.svg "Markup Blitz"
[ASL]: http://www.apache.org/licenses/LICENSE-2.0
[REx]: https://bottlecaps.de/rex
[LALR]: https://en.wikipedia.org/wiki/LALR_parser
[GLR]: https://en.wikipedia.org/wiki/GLR_parser
[rex-parser-benchmark]: https://github.com/GuntherRademacher/rex-parser-benchmark