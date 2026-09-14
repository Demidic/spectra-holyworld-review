#version 330 core

uniform sampler2D Sampler0;

in vec2 TexCoord;
out vec4 OutColor;

void main() {
    float sum = 0.0;
    const int columns = 12;
    const int rows = 8;
    for (int y = 0; y < rows; y++) {
        for (int x = 0; x < columns; x++) {
            vec2 uv = vec2(
                (float(x) + 0.5) / float(columns),
                (float(y) + 0.5) / float(rows)
            );
            sum += texture(Sampler0, uv).r;
        }
    }
    float coverage = sum / float(columns * rows);
    OutColor = vec4(coverage, coverage, coverage, 1.0);
}
