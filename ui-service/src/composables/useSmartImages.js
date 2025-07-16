/**
 * 스마트 이미지 시스템을 위한 Composable
 */
export function useSmartImages() {
    const BASE_IMAGE_PATH = '/images/banners/products/'

    const getProductImage = (product) => {
        // 이미 완전한 URL인 경우 (http로 시작하는 외부 이미지)
        if (product.image && product.image.startsWith('http')) {
            return product.image
        }

        // 이미 완전한 경로인 경우 (/images로 시작)
        if (product.image && product.image.startsWith('/images')) {
            return product.image
        }

        // mainImage가 있으면 기본 경로 붙이기
        if (product.mainImage) {
            return `${BASE_IMAGE_PATH}${product.mainImage}`
        }

        // image 필드가 파일명만 있는 경우
        if (product.image) {
            return `${BASE_IMAGE_PATH}${product.image}`
        }

        // 기본 이미지
        return '/images/banners/products/default-product.jpg'
    }

    const handleImageError = (event) => {
        event.target.src = '/images/banners/products/default-product.jpg'
    }

    const handleImageLoad = (event) => {
        // 필요시 로딩 완료 처리
    }

    return {
        getProductImage,
        handleImageError,
        handleImageLoad
    }
}