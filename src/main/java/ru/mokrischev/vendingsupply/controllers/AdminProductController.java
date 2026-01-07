package ru.mokrischev.vendingsupply.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.mokrischev.vendingsupply.model.entity.Product;
import ru.mokrischev.vendingsupply.services.ProductService;

@Controller
@RequestMapping("/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;

    @Value("${app.upload.path}")
    private String uploadPath;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("products", productService.findAll());
        return "admin/products/list";
    }

    @GetMapping("/new")
    public String newProduct(Model model) {
        model.addAttribute("product", new Product());
        addImageListToModel(model);
        return "admin/products/form";
    }

    @PostMapping("/save")
    public String save(@jakarta.validation.Valid @ModelAttribute Product product,
            org.springframework.validation.BindingResult bindingResult,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam(value = "selectedImage", required = false) String selectedImage,
            Model model) {

        if (bindingResult.hasErrors()) {
            addImageListToModel(model);
            return "admin/products/form";
        }

        try {
            if (!file.isEmpty()) {
                String fileName = org.springframework.util.StringUtils.cleanPath(file.getOriginalFilename());
                java.nio.file.Path path = java.nio.file.Paths.get(uploadPath + fileName);
                java.nio.file.Files.copy(file.getInputStream(), path,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                product.setImageUrl("/uploads/" + fileName);
            } else if (selectedImage != null && !selectedImage.isEmpty()) {
                product.setImageUrl("/uploads/" + selectedImage);
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }

        productService.save(product);
        return "redirect:/admin/products";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        Product product = productService.findById(id);
        if (product == null) {
            return "redirect:/admin/products";
        }
        model.addAttribute("product", product);
        addImageListToModel(model);
        return "admin/products/form";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        productService.softDelete(id);
        return "redirect:/admin/products";
    }

    @PostMapping("/restore/{id}")
    public String restore(@PathVariable Long id) {
        productService.restore(id);
        return "redirect:/admin/products";
    }

    private void addImageListToModel(Model model) {
        java.util.List<String> images = new java.util.ArrayList<>();
        java.nio.file.Path imagesPath = java.nio.file.Paths.get(uploadPath);

        try {
            if (!java.nio.file.Files.exists(imagesPath)) {
                java.nio.file.Files.createDirectories(imagesPath);
            }

            try (java.util.stream.Stream<java.nio.file.Path> paths = java.nio.file.Files.walk(imagesPath)) {
                paths.filter(java.nio.file.Files::isRegularFile)
                        .map(path -> path.getFileName().toString())
                        .filter(name -> !name.equals(".gitkeep"))
                        .forEach(images::add);
            }
        } catch (java.io.IOException e) {
            System.err.println("Could not list images in " + uploadPath + ": " + e.getMessage());
        }
        model.addAttribute("existingImages", images);
    }
}
