# indoqa-fsa

Provides an abstraction layer for acceptors and transducers from [Morfologik](https://github.com/morfologik/) as well as alternative implementations.

The abstraction layer handles the conversion between Strings and bytes, offers support for case-insensitive operations and easier construction of acceptors and transducers.

The alternative implementations work directly on characters, which results in better runtime behaviour and greatly reduced need for garbage collection.