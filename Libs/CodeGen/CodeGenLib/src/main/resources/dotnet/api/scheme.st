package rapture.common;

public enum Scheme {
    <fields; separator=", ">;

    private boolean isPrimitive;

    Scheme() {
        this.isPrimitive = false;
    }

    Scheme(boolean isPrimitive) {
        this.isPrimitive = isPrimitive;
    }

    public boolean isPrimitive() {
        return this.isPrimitive;
    }

    @Override
    public String toString() {
        return this.name().toLowerCase();
    }
    
    public static Scheme getScheme(String name) {
        try {
            return Scheme.valueOf(name.toUpperCase());
          } catch (IllegalArgumentException e) {
            return null;
          }
    }
} 



