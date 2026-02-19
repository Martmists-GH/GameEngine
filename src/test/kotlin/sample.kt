import com.martmists.engine.Input
import com.martmists.engine.Window
import com.martmists.engine.component.Camera
import com.martmists.engine.component.CameraController
import com.martmists.engine.component.DirectionalLight
import com.martmists.engine.component.ImguiRenderer
import com.martmists.engine.component.ModelRenderer
import com.martmists.engine.component.SpriteRenderer
import com.martmists.engine.math.Quat
import com.martmists.engine.math.Vec2
import com.martmists.engine.math.Vec2i
import com.martmists.engine.math.Vec3
import com.martmists.engine.render.DefaultRenderPipeline
import com.martmists.engine.render.WireframeRenderPipeline
import com.martmists.engine.scene.GameObject
import com.martmists.engine.scene.Scene
import com.martmists.engine.sprite.Sprite
import com.martmists.engine.util.Resource
import com.martmists.engine.util.ResourceLoader
import imgui.ImGui
import imgui.type.ImInt
import org.lwjgl.glfw.GLFW.GLFW_KEY_TAB
import org.lwjgl.glfw.GLFW.glfwGetTime
import org.lwjgl.glfw.GLFW.glfwInit
import org.lwjgl.glfw.GLFW.glfwPollEvents
import org.lwjgl.glfw.GLFW.glfwSetErrorCallback
import org.lwjgl.glfw.GLFW.glfwTerminate
import org.lwjgl.glfw.GLFWErrorCallback
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin


fun main() {
    GLFWErrorCallback.createPrint(System.err).set()
    if (!glfwInit()) {
        throw IllegalStateException("Unable to initialize GLFW")
    }

    val win = Window(1280, 720, "Game Window")
    val scene = Scene()

    // Model I used for testing: https://sketchfab.com/3d-models/substance-material-darkmetal-panels-c7a4150166554fc194e9a0cc500af1d2
    val testModel = ResourceLoader.loadModel(Resource("/home/mart/Downloads/substance_material_darkmetal_panels.glb"))
    val testSpritesheet = ResourceLoader.loadSpritesheet(Resource("/home/mart/Documents/test_sprite.png"))!!
    val testSprite = Sprite(testSpritesheet, Vec2i(64, 16), Vec2i(0, 0), Vec2i(16, 0) to Vec2i(48, 16))

    val obj = GameObject("Model")
    obj.addComponent<ModelRenderer>().apply {
        model = testModel.instantiate()
    }
    obj.transform.scale *= 0.2f

    val obj2 = GameObject("Sprite")
    obj2.addComponent<SpriteRenderer>().apply {
        sprite = testSprite
        stretch = Vec2(1.5f, 1f)
    }
    obj2.transform.scale *= 2f
    obj2.transform.translation = Vec3(2f, 0f, 0f)

    val sun = GameObject("Sun")
    sun.addComponent<DirectionalLight>().apply {
        intensity = 1f
    }
    sun.transform.translation = Vec3(-2f, 2f, 2f)
    val defaultSunRot = Quat.Identity.lookAt(Vec3(-1f, -1f, -1f))
    sun.transform.rotation = defaultSunRot

    val camera = GameObject("Camera")
    camera.addComponent<Camera>()
    camera.addComponent<CameraController>()
    camera.transform.translation = Vec3(2f, 2f, 2f)
    camera.getComponent<Camera>().lookAt(obj)

    val imgui = GameObject("ImGui")
    imgui.addComponent<ImguiRenderer>().apply {
        val currentRenderer = ImInt(0)
        val currentCamSpeed = floatArrayOf(camera.getComponent<CameraController>().speed)
        val currentTime = floatArrayOf(sun.transform.rotation.toEuler().x)

        renderCallback = {
            if (ImGui.combo("Renderer", currentRenderer, arrayOf("Default", "Wireframe"))) {
                if (currentRenderer.get() == 0) {
                    win.viewport.pipeline = DefaultRenderPipeline
                } else {
                    win.viewport.pipeline = WireframeRenderPipeline
                }
            }

            if (ImGui.sliderFloat("Camera Speed", currentCamSpeed, 0.1f, 5f)) {
                camera.getComponent<CameraController>().speed = currentCamSpeed[0]
            }

            if (ImGui.sliderFloat("Time", currentTime, 0f, (2 * PI).toFloat())) {
                val sourceVec = Vec3(0f, 0f, 1f)
                val targetVec = Vec3(cos(currentTime[0]), sin(currentTime[0]), 0f)
                val cross = sourceVec cross targetVec
                val angle = acos(sourceVec dot targetVec)
                sun.transform.worldRotation = Quat(cos(angle / 2), sin(angle / 2) * cross.x, sin(angle / 2) * cross.y, sin(angle / 2) * cross.z)
            }

            if (currentRenderer.get() == 0) {
                if (ImGui.checkbox("Disable Ambient", DefaultRenderPipeline.Settings.disableAmbient)) {
                    DefaultRenderPipeline.Settings.disableAmbient = !DefaultRenderPipeline.Settings.disableAmbient
                }
                if (ImGui.checkbox("Disable Diffuse", DefaultRenderPipeline.Settings.disableDiffuse)) {
                    DefaultRenderPipeline.Settings.disableDiffuse = !DefaultRenderPipeline.Settings.disableDiffuse
                }
                if (ImGui.checkbox("Disable Emissive", DefaultRenderPipeline.Settings.disableEmissive)) {
                    DefaultRenderPipeline.Settings.disableEmissive = !DefaultRenderPipeline.Settings.disableEmissive
                }
                if (ImGui.checkbox("Disable Specular", DefaultRenderPipeline.Settings.disableSpecular)) {
                    DefaultRenderPipeline.Settings.disableSpecular = !DefaultRenderPipeline.Settings.disableSpecular
                }
                if (ImGui.checkbox("Disable Normal", DefaultRenderPipeline.Settings.disableNormal)) {
                    DefaultRenderPipeline.Settings.disableNormal = !DefaultRenderPipeline.Settings.disableNormal
                }
                if (ImGui.checkbox("Disable Displacement", DefaultRenderPipeline.Settings.disableDisplacement)) {
                    DefaultRenderPipeline.Settings.disableDisplacement = !DefaultRenderPipeline.Settings.disableDisplacement
                }
                if (ImGui.checkbox("Use Ray-March Displacement", DefaultRenderPipeline.Settings.useRayMarchDisplacement)) {
                    DefaultRenderPipeline.Settings.useRayMarchDisplacement = !DefaultRenderPipeline.Settings.useRayMarchDisplacement
                }
                if (ImGui.checkbox("Render Normals", DefaultRenderPipeline.Settings.renderNormals)) {
                    DefaultRenderPipeline.Settings.renderNormals = !DefaultRenderPipeline.Settings.renderNormals
                }
            }
        }
    }

    scene.addObject(imgui)
    scene.addObject(obj)
    scene.addObject(obj2)
    scene.addObject(sun)
    scene.addObject(camera)

    win.viewport.scene = scene

    var lastTime = glfwGetTime()
    while (!win.shouldClose()) {
        val now = glfwGetTime()
        val delta = (now - lastTime).toFloat()

        win.viewport.scene.objects.onEach { it.preUpdate(delta) }
        win.viewport.scene.objects.onEach { it.onUpdate(delta) }
        win.viewport.scene.objects.onEach { it.postUpdate(delta) }

        if (Input.isAnyKeyDown(GLFW_KEY_TAB)) {
            camera.getComponent<Camera>().lookAt(obj)
        }

        win.render()

        lastTime = now
        glfwPollEvents()
    }

    glfwTerminate()
    glfwSetErrorCallback(null)?.free()
}
