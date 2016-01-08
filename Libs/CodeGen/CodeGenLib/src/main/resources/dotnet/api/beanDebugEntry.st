    {
        sb.append(" <fieldName>= ");
        Object o = <fieldName>;
        if (o != null) {
            if (o instanceof java.util.Collection) {
                sb.append("{ ");
                for (Object oo : (java.util.Collection\<?\>) o) {
                    if (oo == null) sb.append("null");
                    else if (oo instanceof Debugable) sb.append(((Debugable) oo).debug());
                    else sb.append(oo.toString()).append(", ");
                }
                sb.append(" } ");
            } else {
                if (o instanceof Debugable) sb.append(((Debugable) o).debug());
                else sb.append(o.toString());
            }
        }
    }
