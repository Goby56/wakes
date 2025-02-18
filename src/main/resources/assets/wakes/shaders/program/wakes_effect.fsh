#version 150

// The main texture
uniform sampler2D DiffuseSampler;
// The depth map
uniform sampler2D WakeSampler;
uniform sampler2D DepthSampler;
//uniform sampler2D WakeSampler;
// uniform sampler2D DynamicTexture;

// Position of the camera
uniform vec3 CameraPosition;
// Position of the center of the ping effect
uniform vec3 Center;
// Time in seconds (+ tick delta)

// The magic matrix to get world coordinates from pixel ones
uniform mat4 InverseTransformMatrix;
// The size of the viewport (typically, [0,0,1080,720])
uniform ivec4 ViewPort;

in vec2 texCoord;
in vec4 vPosition;

out vec4 fragColor;

vec4 CalcEyeFromWindow(in float depth)
{
  // derived from https://www.khronos.org/opengl/wiki/Compute_eye_space_from_window_space
  // ndc = Normalized Device Coordinates
  vec3 ndcPos;
  ndcPos.xy = ((2.0 * gl_FragCoord.xy) - (2.0 * ViewPort.xy)) / (ViewPort.zw) - 1;
  ndcPos.z = (2.0 * depth - gl_DepthRange.near - gl_DepthRange.far) / (gl_DepthRange.far - gl_DepthRange.near);
  vec4 clipPos = vec4(ndcPos, 1.);
  vec4 homogeneous = InverseTransformMatrix * clipPos;
  vec4 eyePos = vec4(homogeneous.xyz / homogeneous.w, homogeneous.w);
  return eyePos;
}

void main()
{
    vec4 tex = texture(DiffuseSampler, texCoord);

    vec3 ndc = vPosition.xyz / vPosition.w; //perspective divide/normalize
    vec2 viewportCoord = ndc.xy * 0.5 + 0.5; //ndc is -1 to 1 in GL. scale for 0 to 1

    // Depth fading
    float sceneDepth = texture(DepthSampler, viewportCoord).x;
    float otherSceneDepth = texture(WakeSampler, viewportCoord).x;
    vec3 pixelPosition = CalcEyeFromWindow(sceneDepth).xyz + CameraPosition;

    float d = distance(pixelPosition, Center);
    //vec4 wake = texture(WakeSampler, texCoord);
    fragColor = vec4(tex.rgb + vec3(d / 100), 1);
    //fragColor = vec4(wake.rgb, 1);
}
