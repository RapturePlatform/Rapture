        if (<name> != null) {
            <if (encoding)>
            builder.append( encode(<name>.toString()) );
            <else>
            builder.append( <name>.toString() );
            <endif>
            builder.append(<separator>);
        }
