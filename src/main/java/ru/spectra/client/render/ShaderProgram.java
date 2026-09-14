package ru.spectra.client.render;
import ru.spectra.client.Spectra;
import ru.spectra.client.resource.ClasspathResource;
import ru.spectra.client.resource.Reloadable;
import ru.spectra.client.resource.ResourceSource;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntConsumer;
import org.lwjgl.opengl.GL33;

public class ShaderProgram implements Reloadable {
    private static final Map<String, Integer> sharedClasspathShaders = new HashMap<>();
    public static final IntConsumer noOpCallback = i -> {
    };
    public final ResourceSource fragmentSource;
    public final ResourceSource vertexSource;
    public Integer programId;

    public IntConsumer compileCallback = noOpCallback;

    public void bind() {
        GL33.glUseProgram(getProgramId());
    }

    public void unbind() {
        GL33.glUseProgram(0);
    }

    @Override
    public void reload() {
        release();
    }

    public void release() {
        if (this.programId != null) {
            GL33.glDeleteProgram(this.programId.intValue());
            this.programId = null;
        }
    }

    public ShaderUniform uniform(String str) {
        ShaderUniform class228Var = new ShaderUniform(str);
        if (this.programId != null) {
            class228Var.resolveLocation(this.programId.intValue());
        } else {
            addCompileCallback(class228Var.programCompileCallback());
        }
        return class228Var;
    }

    public int getProgramId() {
        if (this.programId != null) {
            return this.programId.intValue();
        }
        Integer numValueOf = Integer.valueOf(createProgram());
        this.programId = numValueOf;
        return numValueOf.intValue();
    }

    public void initializeWithVertexSource(CharSequence vertexCode) {
        if (this.programId != null) {
            return;
        }
        this.programId = Integer.valueOf(createProgram(vertexCode));
    }

    public int createProgram() {
        return createProgram(null);
    }

    private int createProgram(CharSequence vertexCode) {
        int iGlCreateProgram = GL33.glCreateProgram();
        String vertexKey = vertexCode == null
                ? sharedShaderKey(35633, this.vertexSource)
                : null;
        String fragmentKey = sharedShaderKey(35632, this.fragmentSource);
        int iMethod003 = 0;
        int iMethod004 = 0;
        try {
            iMethod003 = vertexCode == null
                    ? compileShader(35633, this.vertexSource)
                    : compileShader(35633, vertexCode, this.vertexSource);
            iMethod004 = compileShader(35632, this.fragmentSource);
            GL33.glAttachShader(iGlCreateProgram, iMethod003);
            GL33.glAttachShader(iGlCreateProgram, iMethod004);
            GL33.glLinkProgram(iGlCreateProgram);
            if (GL33.glGetProgrami(iGlCreateProgram, 35714) == 0) {
                throw new IllegalStateException("Could not link program: " + GL33.glGetProgramInfoLog(iGlCreateProgram));
            }
            notifyCompiled(iGlCreateProgram);
            return iGlCreateProgram;
        } catch (RuntimeException error) {
            GL33.glDeleteProgram(iGlCreateProgram);
            throw error;
        } finally {
            if (iMethod003 != 0 && vertexKey == null) {
                GL33.glDeleteShader(iMethod003);
            }
            if (iMethod004 != 0 && fragmentKey == null) {
                GL33.glDeleteShader(iMethod004);
            }
        }
    }

    public int compileShader(int i, ResourceSource class178Var) {
        String sharedKey = sharedShaderKey(i, class178Var);
        if (sharedKey != null) {
            synchronized (sharedClasspathShaders) {
                Integer existing = sharedClasspathShaders.get(sharedKey);
                if (existing != null && GL33.glIsShader(existing.intValue())) {
                    return existing.intValue();
                }
                char[] source = readUtf8(class178Var);
                try {
                    int compiled = compileShader(
                            i,
                            CharBuffer.wrap(source).asReadOnlyBuffer(),
                            class178Var
                    );
                    sharedClasspathShaders.put(sharedKey, Integer.valueOf(compiled));
                    return compiled;
                } finally {
                    Arrays.fill(source, '\0');
                }
            }
        }
        char[] source = readUtf8(class178Var);
        try {
            return compileShader(
                    i,
                    CharBuffer.wrap(source).asReadOnlyBuffer(),
                    class178Var
            );
        } finally {
            Arrays.fill(source, '\0');
        }
    }

    static String sharedShaderKey(int type, ResourceSource source) {
        if (source instanceof ClasspathResource classpath) {
            return type + ":" + classpath.normalizedPath();
        }
        return null;
    }

    static char[] readUtf8(ResourceSource source) {
        byte[] encoded = source.bytes();
        try {
            CharBuffer decoded = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(encoded));
            char[] value = new char[decoded.remaining()];
            decoded.get(value);
            return value;
        } catch (CharacterCodingException error) {
            throw new IllegalArgumentException(
                    "Shader source is not valid UTF-8",
                    error
            );
        } finally {
            Arrays.fill(encoded, (byte) 0);
        }
    }

    private int compileShader(
            int i,
            CharSequence source,
            Object description
    ) {
        String str;
        switch (i) {
            case 35632:
                str = "FRAGMENT";
                break;
            case 35633:
                str = "VERTEX";
                break;
            default:
                str = "TYPE_" + i;
                break;
        }
        Spectra.LOGGER.info(
                "Compiling {} shader from resource: {} ({})",
                new Object[]{
                        str,
                        description,
                        description.getClass().getName()
                }
        );
        int iGlCreateShader = GL33.glCreateShader(i);
        GL33.glShaderSource(iGlCreateShader, source);
        GL33.glCompileShader(iGlCreateShader);
        if (GL33.glGetShaderi(iGlCreateShader, 35713) == 0) {
            throw new IllegalStateException("Couldn't compile shader: " + GL33.glGetShaderInfoLog(iGlCreateShader));
        }
        return iGlCreateShader;
    }

    public void notifyCompiled(int i) {
        this.compileCallback.accept(i);
    }

    public void addCompileCallback(IntConsumer intConsumer) {
        IntConsumer intConsumer2 = this.compileCallback;
        this.compileCallback = i -> {
            intConsumer2.accept(i);
            intConsumer.accept(i);
        };
    }

    public ShaderProgram(ResourceSource class178Var, ResourceSource class178Var2) {
        this.fragmentSource = class178Var;
        this.vertexSource = class178Var2;
    }
}
